package com.jiefzz.ejoker.eventing.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandStatus;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import com.jiefzz.ejoker.domain.IAggregateRootFactory;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.EventAppendResult;
import com.jiefzz.ejoker.eventing.EventCommittingContext;
import com.jiefzz.ejoker.eventing.IEventService;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.io.IOHelper.IOActionExecutionContext;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.system.util.extension.KeyValuePair;
import com.jiefzz.ejoker.z.common.task.context.EJokerReactThreadScheduler;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

@EService
public class DefaultEventService
		implements IEventService {

	private final static Logger logger = LoggerFactory.getLogger(DefaultEventService.class);

	private final Map<String, EventMailBox> eventMailboxDict = new ConcurrentHashMap<>();

	@Dependence
	IProcessingCommandHandler processingCommandHandler;
	@Dependence
	IJSONConverter jsonSerializer;
	@Dependence
	IMemoryCache memoryCache;
	@Dependence
	IAggregateRootFactory aggregateRootFactory;
	@Dependence
	IAggregateStorage aggregateStorage;
	@Dependence
	IEventStore eventStore;
	@Dependence
	IMessagePublisher<DomainEventStreamMessage> domainEventPublisher;
	@Dependence
	IOHelper ioHelper;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private EJokerReactThreadScheduler reactThreadScheduler;

	private final int batchSize = 1;

	@Override
	public void commitDomainEventAsync(EventCommittingContext context) {
		String uniqueId = context.aggregateRoot.getUniqueId();
		EventMailBox eventMailbox = MapHelper.getOrAddConcurrent(eventMailboxDict, uniqueId, () -> new EventMailBox(uniqueId, batchSize,
				committingContexts -> {
					if (committingContexts == null || committingContexts.size() == 0)
						return;
					if (eventStore.isSupportBatchAppendEvent())
						batchPersistEventAsync(committingContexts, 0);
					else
						persistEventOneByOne(committingContexts);
			}, reactThreadScheduler));
		eventMailbox.enqueueMessage(context);
		refreshAggregateMemoryCache(context);

	}

	/**
	 * 封装DomainEventStream，并向q端发布领域事件
	 * 
	 * @param processingCommand
	 * @param eventStream
	 * @param retryTimes
	 */
	@Override
	public void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStream eventStream) {
		if (null == eventStream.getItems() || 0 == eventStream.getItems().size())
			eventStream.setItems(processingCommand.getItems());
		DomainEventStreamMessage domainEventStreamMessage = new DomainEventStreamMessage(
				processingCommand.getMessage().getId(),
				eventStream.getAggregateRootId(),
				eventStream.getVersion(),
				eventStream.getAggregateRootTypeName(),
				eventStream.getEvents(),
				eventStream.getItems());
		publishDomainEventAsync(processingCommand, domainEventStreamMessage);
	}

	private void refreshAggregateMemoryCache(EventCommittingContext context) {
		try {
			context.aggregateRoot.acceptChanges(context.eventStream.getVersion());
			memoryCache.set(context.aggregateRoot);
		} catch (Exception ex) {
			logger.error(
					String.format("Refresh aggregate memory cache failed for event stream:{}", context.eventStream),
					ex);
		}
	}

	private void cleanInactiveMailbox() {
		// TODO 这个位置注意性能。。。。
		List<KeyValuePair<String, EventMailBox>> inactiveList = new ArrayList<KeyValuePair<String, EventMailBox>>();

		Set<Entry<String, EventMailBox>> entrySet = eventMailboxDict.entrySet();
		for (KeyValuePair<String, EventMailBox> pair : inactiveList) {
			EventMailBox mailbox = pair.getValue();
			if (mailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT) && !mailbox.isRunning())
				inactiveList.add(new KeyValuePair<String, EventMailBox>(pair.getKey(), mailbox));
		}

		for (KeyValuePair<String, EventMailBox> pair : inactiveList) {
			EventMailBox mailbox = pair.getValue();
			if(!mailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT)) {
				// 在上面判断其达到空闲条件后，又被重新使能（临界情况）
				// 放弃本次操作
				continue;
			}
			if (null != eventMailboxDict.remove(pair.getKey())) {
				logger.debug("Removed inactive event mailbox, aggregateRootId: {}", pair.getKey());
			}
		}
	}

	private void completeCommand(ProcessingCommand processingCommand, CommandResult commandResult) {
		processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
	}

	private void batchPersistEventAsync(List<EventCommittingContext> committingContexts, int retryTimes) {
		// 异步批量持久化
		throw new InfrastructureRuntimeException("批量持久化没完成！");
	}

	private void persistEventOneByOne(List<EventCommittingContext> contextList) {
		// 逐个持久化
		concatContexts(contextList);
		persistEventAsync(contextList.get(0));

	}

	private void persistEventAsync(final EventCommittingContext context) {
		
		ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResultBase>() {

			@Override
			public String getAsyncActionName() {
				return "persistEventAsync";
			}

			@Override
			public Future<AsyncTaskResultBase> asyncAction() throws IOException {
				return eventStore.appendAsync(context.eventStream);
			}

			@Override
			public void faildLoopAction() {
				ioHelper.tryAsyncAction(this);
			}

			@Override
			public void finishAction(AsyncTaskResultBase result) {
				AsyncTaskResult<EventAppendResult> realrResult = (AsyncTaskResult<EventAppendResult> )result;
				switch (realrResult.getData()) {
				case Success:
					logger.debug("Persist event success, {}", context.eventStream);
					
//					new Thread(new Runnable() {
//						@Override
//						public void run() {
//							publishDomainEventAsync(context.processingCommand,
//									context.eventStream);
//						}
//					}).start();
					systemAsyncHelper.submit(() -> publishDomainEventAsync(context.processingCommand,
							context.eventStream));
					
					if (null != context.next)
						persistEventAsync(context.next);
					else
						context.eventMailBox.tryRun(true);
					break;
				case DuplicateEvent:
					if(context.eventStream.getVersion() - 1 == 0) {
						handleFirstEventDuplicationAsync(context);
					} else {
						logger.warn("Persist event has concurrent version conflict, eventStream: {}", context.eventStream);
					}
					break;
				case DuplicateCommand:
					break;
				default:
					assert false;
					break;
				}
			}

			@Override
			public String getContextInfo() {
				return String.format("[eventStream: %s]", context.eventStream.toString());
			}

			@Override
			public void faildAction(Exception ex) {
				logger.error(
						"Persist event has unknown exception, the code should not be run to here, errorMessage: {}",
						ex.getMessage());
			}});

	}
	
	/**
	 * 遇到Version为1的时间的重复的时候，做特殊处理。
	 * @param context
	 */
	private void handleFirstEventDuplicationAsync(final EventCommittingContext context) {
		
		final DomainEventStream eventStream = context.eventStream;
		
		ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<DomainEventStream>>(){

			@Override
			public String getAsyncActionName() {
				return "FindFirstEventByVersion";
			}

			@Override
			public Future<AsyncTaskResult<DomainEventStream>> asyncAction() throws IOException {
				return eventStore.findAsync(eventStream.getAggregateRootId(), 1);
			}

			@Override
			public void faildLoopAction() {
				ioHelper.tryAsyncAction(this);
			}

			@Override
			public void finishAction(AsyncTaskResult<DomainEventStream> result) {
				AsyncTaskResult<DomainEventStream> realResult = (AsyncTaskResult<DomainEventStream> )result;
				DomainEventStream firstEventStream = realResult.getData();
				if(null != firstEventStream) {
					//判断是否是同一个command，如果是，则再重新做一遍发布事件；
                    //之所以要这样做，是因为虽然该command产生的事件已经持久化成功，但并不表示事件也已经发布出去了；
                    //有可能事件持久化成功了，但那时正好机器断电了，则发布事件都没有做；
					if(context.processingCommand.getMessage().getId().equals(firstEventStream.getCommandId())) {
						resetCommandMailBoxConsumingSequence(context, context.processingCommand.getSequence());
						publishDomainEventAsync(context.processingCommand, firstEventStream);
					} else {
						
					}
				} else {
					
				}
			}

			@Override
			public void faildAction(Exception ex) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}

	private void resetCommandMailBoxConsumingSequence(EventCommittingContext context, long consumingSequence) {

		EventMailBox eventMailBox = context.eventMailBox;
		ProcessingCommand processingCommand = context.processingCommand;
		ICommand command = processingCommand.getMessage();
		ProcessingCommandMailbox commandMailBox = processingCommand.getMailbox();

		commandMailBox.pause();
		try {
			refreshAggregateMemoryCacheToLatestVersion(context.eventStream.getAggregateRootTypeName(),
					context.eventStream.getAggregateRootId());
			commandMailBox.resetConsumingSequence(consumingSequence);
			eventMailBox.clear();
			eventMailBox.exit();
			logger.debug(
					"ResetCommandMailBoxConsumingSequence success, commandId: {}, aggregateRootId: {}, consumingSequence: {}",
					command.getId(), command.getAggregateRootId(), consumingSequence);
		} catch (Exception ex) {
			logger.error(String.format(
					"ResetCommandMailBoxConsumingOffset has unknown exception, commandId: %s, aggregateRootId: %s",
					command.getId(), command.getAggregateRootId()), ex);
		} finally {
			commandMailBox.resume();
		}
	}

	private void refreshAggregateMemoryCacheToLatestVersion(String aggregateRootTypeName, String aggregateRootId) {
		memoryCache.refreshAggregateFromEventStore(aggregateRootTypeName, aggregateRootId);
	}

	private void publishDomainEventAsync(final ProcessingCommand processingCommand,
			final DomainEventStreamMessage eventStream) {

		ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResultBase>() {

			@Override
			public String getAsyncActionName() {
				return "publishEventAsync";
			}

			@Override
			public Future<AsyncTaskResultBase> asyncAction() throws IOException {
				return domainEventPublisher.publishAsync(eventStream);
			}

			@Override
			public void faildLoopAction() {
				ioHelper.tryAsyncAction(this);
			}

			@Override
			public void finishAction(AsyncTaskResultBase result) {
				
				logger.debug("Publish event success, {}", eventStream.toString());

				String commandHandleResult = processingCommand.getCommandExecuteContext().getResult();
				CommandResult commandResult = new CommandResult(
						CommandStatus.Success,
						processingCommand.getMessage().getId(),
						eventStream.getAggregateRootId(),
						commandHandleResult,
						String.class.getName());
				completeCommand(processingCommand, commandResult);
				
			}

			@Override
			public String getContextInfo() {
				return String.format("[eventStream: %s]", eventStream.toString());
			}
			
			@Override
			public void faildAction(Exception ex) {
				logger.error(
						"Publish event has unknown exception, the code should not be run to here, errorMessage: {}",
						ex.getMessage());
			}});
		
	}

	/**
	 * concat the EventCommittingContext list
	 * 
	 * @param contextList
	 */
	private void concatContexts(List<EventCommittingContext> contextList) {
		Iterator<EventCommittingContext> iterator = contextList.iterator();
		EventCommittingContext previous = null;
		if (iterator.hasNext())
			previous = iterator.next();
		while (iterator.hasNext()) {
			EventCommittingContext current = iterator.next();
			previous.next = current;
			previous = current;
		}
	}
}