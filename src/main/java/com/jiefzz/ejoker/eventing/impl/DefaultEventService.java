package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandStatus;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRootFactory;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import com.jiefzz.ejoker.eventing.EventAppendResult;
import com.jiefzz.ejoker.eventing.EventCommittingConetxt;
import com.jiefzz.ejoker.eventing.IEventService;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.action.Action;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.system.util.extension.KeyValuePair;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;

@EService
public class DefaultEventService implements IEventService {

	private final static Logger logger = LoggerFactory.getLogger(DefaultEventService.class);

	private final Lock lock4tryCreateEventMailbox = new ReentrantLock();

	private final Map<String, EventMailBox> eventMailboxDict = new ConcurrentHashMap<String, EventMailBox>();

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

	private final int batchSize = 1;

	@Override
	public void commitDomainEventAsync(EventCommittingConetxt context) {
		String uniqueId = context.aggregateRoot.getUniqueId();
		EventMailBox eventMailbox;
		if (null == (eventMailbox = eventMailboxDict.getOrDefault(uniqueId, null))) {
			lock4tryCreateEventMailbox.lock();
			try {
				if (!eventMailboxDict.containsKey(uniqueId)) {
					eventMailboxDict.put(uniqueId, eventMailbox = new EventMailBox(uniqueId, batchSize,
							new EventMailBox.EventMailBoxHandler<List<EventCommittingConetxt>>() {
								// 暂不使用lambda表达式。。。。
								@Override
								public void handleMessage(List<EventCommittingConetxt> committingContexts) {
									if (committingContexts == null || committingContexts.size() == 0)
										return;
									if (eventStore.isSupportBatchAppendEvent())
										DefaultEventService.this.batchPersistEventAsync(committingContexts, 0);
									else
										DefaultEventService.this.persistEventOneByOne(committingContexts);
								}
							}));
				} else
					eventMailbox = eventMailboxDict.get(uniqueId);
			} finally {
				lock4tryCreateEventMailbox.unlock();
			}
		}
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
				processingCommand.getMessage().getId(), eventStream.getAggregateRootId(), eventStream.getVersion(),
				eventStream.getAggregateRootTypeName(), eventStream.getEvents(), eventStream.getItems());
		publishDomainEventAsync(processingCommand, domainEventStreamMessage, 0);
	}

	private void refreshAggregateMemoryCache(EventCommittingConetxt context) {
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
				logger.info("Removed inactive event mailbox, aggregateRootId: {}", pair.getKey());
			}
		}
	}

	private void completeCommand(ProcessingCommand processingCommand, CommandResult commandResult) {
		processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
	}

	private void batchPersistEventAsync(List<EventCommittingConetxt> committingContexts, int retryTimes) {
		// 异步批量持久化
		throw new InfrastructureRuntimeException("批量持久化没完成！");
	}

	private void persistEventOneByOne(List<EventCommittingConetxt> contextList) {
		// 逐个持久化
		concatContexts(contextList);
		persistEventAsync(contextList.get(0), 0);

	}

	private void persistEventAsync(final EventCommittingConetxt context, int currentRetryTimes) {


		ioHelper.tryAsyncActionRecursively("persistEventAsync", new IAsyncTask<Future<AsyncTaskResultBase>>() {
			// AsyncAction
			public Future<AsyncTaskResultBase> call() throws Exception {
				return DefaultEventService.this.eventStore.appendAsync(context.eventStream);
			}
		}, new Action<Integer>() {
			// MainAction
			@Override
			public void trigger(Integer nextRetryTimes) {
				DefaultEventService.this.persistEventAsync(context, nextRetryTimes);
			}
		}, new Action<AsyncTaskResultBase>() {
			// SuccessAction
			public void trigger(AsyncTaskResultBase result) {
				AsyncTaskResult<EventAppendResult> realrResult = (AsyncTaskResult<EventAppendResult> )result;
				switch (realrResult.getData()) {
				case Success:
					logger.debug("Persist event success, {}", context.eventStream);
					new Thread(new Runnable() {
						@Override
						public void run() {
							DefaultEventService.this.publishDomainEventAsync(context.processingCommand,
									context.eventStream);
						}
					}).start();
					if (null != context.next)
						persistEventAsync(context.next, 0);
					else
						context.eventMailBox.tryRun(true);
					break;
				case DuplicateEvent:
					break;
				case DuplicateCommand:
					break;
				default:
					break;
				}
			}
		}, new Callable<String>() {
			// GetContextInfoAction
			public String call() {
				return String.format("[eventStream: %s]", context.eventStream.toString());
			}
		}, new Action<String>() {
			// FailedAction
			@Override
			public void trigger(String errorMessage) {
				logger.error(
						"Persist event has unknown exception, the code should not be run to here, errorMessage: {}",
						errorMessage);
			}
		}, currentRetryTimes, true);

	}

	private void publishDomainEventAsync(final ProcessingCommand processingCommand,
			final DomainEventStreamMessage eventStream, int currentRetryTimes) {

		ioHelper.tryAsyncActionRecursively("publishDomainEventAsync", new IAsyncTask<Future<AsyncTaskResultBase>>() {
			public Future<AsyncTaskResultBase> call() throws Exception {
				return DefaultEventService.this.domainEventPublisher.publishAsync(eventStream);
			}
		}, new Action<Integer>() {
			@Override
			public void trigger(Integer nextRetryTimes) {
				DefaultEventService.this.publishDomainEventAsync(processingCommand, eventStream, nextRetryTimes);
			}
		}, new Action<AsyncTaskResultBase>() {
			public void trigger(AsyncTaskResultBase parameter) {
				logger.debug("Publish event success, {}", eventStream.toString());

				String commandHandleResult = processingCommand.getCommandExecuteContext().getResult();
				CommandResult commandResult = new CommandResult(CommandStatus.Success,
						processingCommand.getMessage().getId(), eventStream.getAggregateRootId(), commandHandleResult,
						String.class.getName());
				completeCommand(processingCommand, commandResult);
			}
		}, new Callable<String>() {
			public String call() {
				return String.format("[eventStream: %s]", eventStream.toString());
			}
		}, new Action<String>() {
			@Override
			public void trigger(String errorMessage) {
				logger.error(
						"Publish event has unknown exception, the code should not be run to here, errorMessage: {}",
						errorMessage);

			}
		}, currentRetryTimes, true);
	}

	/**
	 * concat the EventCommittingContext list
	 * 
	 * @param contextList
	 */
	private void concatContexts(List<EventCommittingConetxt> contextList) {
		Iterator<EventCommittingConetxt> iterator = contextList.iterator();
		EventCommittingConetxt previous = null;
		if (iterator.hasNext())
			previous = iterator.next();
		while (iterator.hasNext()) {
			EventCommittingConetxt current = iterator.next();
			previous.next = current;
			previous = current;
		}
	}
}