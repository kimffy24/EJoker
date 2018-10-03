package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.io.IOHelper.IOActionExecutionContext;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.task.context.EJokerReactThreadScheduler;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;

@EService
public class DefaultEventService implements IEventService {

	private final static Logger logger = LoggerFactory.getLogger(DefaultEventService.class);

	private final Map<String, EventMailBox> eventMailboxDict = new ConcurrentHashMap<>();

	@Dependence
	private IProcessingCommandHandler processingCommandHandler;
	@Dependence
	private IJSONConverter jsonSerializer;
	@Dependence
	private IScheduleService scheduleService;
	@Dependence
	private IMemoryCache memoryCache;
	@Dependence
	private IAggregateRootFactory aggregateRootFactory;
	@Dependence
	private IAggregateStorage aggregateStorage;
	@Dependence
	private IEventStore eventStore;
	@Dependence
	private IMessagePublisher<DomainEventStreamMessage> domainEventPublisher;
	@Dependence
	private IOHelper ioHelper;

	@Dependence
	private EJokerReactThreadScheduler reactThreadScheduler;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("{}@{}#{}", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				() -> cleanInactiveMailbox(),
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT);
	}

	@Override
	public void commitDomainEventAsync(EventCommittingContext context) {
		String uniqueId = context.getAggregateRoot().getUniqueId();
		EventMailBox eventMailbox = MapHelper.getOrAddConcurrent(
				eventMailboxDict,
				uniqueId,
				() -> new EventMailBox(uniqueId,
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


	private void batchPersistEventAsync(List<EventCommittingContext> committingContexts, int retryTimes) {

		LinkedHashSet<DomainEventStream> domainEventStreams = new LinkedHashSet<>();
		ForEachUtil.processForEach(committingContexts, (item) -> domainEventStreams.add(item.getEventStream()));
		
		ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<EventAppendResult>>() {

			@Override
			public String getAsyncActionName() {
				return "BatchPersistEventAsync";
			}

			@Override
			public AsyncTaskResult<EventAppendResult> asyncAction() throws Exception {
				return eventStore.batchAppendAsync(domainEventStreams).get();
			}

			@Override
			public void finishAction(AsyncTaskResult<EventAppendResult> result) {

				EventCommittingContext firstEventCommittingContext = committingContexts.get(0);
				EventMailBox eventMailBox = firstEventCommittingContext.eventMailBox;
				EventAppendResult appendResult = result.getData();
				
				if(EventAppendResult.Success.equals(appendResult)) {
					
					logger.debug(
							"Batch persist event success, aggregateRootId: {}, eventStreamCount: {}",
							eventMailBox.getAggregateRootId(),
							committingContexts.size()
					);
					
					systemAsyncHelper.submit(() -> {
						ForEachUtil.processForEach(committingContexts,
								(context) -> publishDomainEventAsync(context.getProcessingCommand(), context.getEventStream()));
					});
					
					eventMailBox.tryRun(true);
					
				} else if (EventAppendResult.DuplicateEvent.equals(appendResult)) {
					
					long version = firstEventCommittingContext.getEventStream().getVersion();
					if(1l == version) {
						
						handleFirstEventDuplicationAsync(firstEventCommittingContext);
						
					} else {
						
                        logger.warn(
                        		"Batch persist event has concurrent version conflict, first eventStream: {}, batchSize: {}",
                        		jsonSerializer.convert(firstEventCommittingContext.getEventStream()),
                        		committingContexts.size());
                        /// TODO .ConfigureAwait(false) @await
                        resetCommandMailBoxConsumingSequence(
                        		firstEventCommittingContext,
                        		firstEventCommittingContext.getProcessingCommand().getSequence())
                        	.get();
                        
					}
					
				} else if (EventAppendResult.DuplicateCommand.equals(appendResult)) {
					
                    persistEventOneByOne(committingContexts);
                    
                }
			}
			
			@Override
			public String getContextInfo() {
				return String.format("[contextListCount: %d]", committingContexts.size());
			}

			@Override
			public void faildAction(Exception ex) {
				logger.error(String.format("Batch persist event has unknown exception, the code should not be run to here, errorMessage: {0}", ex.getMessage()), ex);
			}
			
		});
		
	}
	
	private void persistEventOneByOne(List<EventCommittingContext> contextList) {
		// 逐个持久化
		concatContexts(contextList);
		persistEventAsync(contextList.get(0));

	}

	private void persistEventAsync(final EventCommittingContext context) {
		
		ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<EventAppendResult>>() {

			@Override
			public String getAsyncActionName() {
				return "PersistEventAsync";
			}

			@Override
			public AsyncTaskResult<EventAppendResult> asyncAction() throws Exception {
				return eventStore.appendAsync(context.getEventStream()).get();
			}

			@Override
			public void finishAction(AsyncTaskResult<EventAppendResult> realrResult) {
				switch (realrResult.getData()) {
				case Success:
					logger.debug("Persist event success, {}", jsonSerializer.convert(context.getEventStream()));
					
					systemAsyncHelper.submit(() -> publishDomainEventAsync(context.getProcessingCommand(),
							context.getEventStream()));
					
					if (null != context.next)
						persistEventAsync(context.next);
					else
						context.eventMailBox.tryRun(true);
					
					break;
					
				case DuplicateEvent:
					if(context.getEventStream().getVersion() - 1 == 0) {
						handleFirstEventDuplicationAsync(context);
					} else {
						logger.warn("Persist event has concurrent version conflict, eventStream: {}", jsonSerializer.convert(context.getEventStream()));
						
						/// TODO .ConfigureAwait(false) @await
						resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence())
							.get();
					}
					break;
				case DuplicateCommand:

                    logger.warn(
                    		"Persist event has duplicate command, eventStream: {}",
                    		jsonSerializer.convert(context.getEventStream()));

					/// TODO .ConfigureAwait(false) @await
                    resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence() + 1)
                    	.get();
                    tryToRepublishEventAsync(context);
					break;
				default:
					assert false;
					break;
				}
			}

			@Override
			public String getContextInfo() {
				return String.format("[eventStream: %s]", jsonSerializer.convert(context.getEventStream()));
			}

			@Override
			public void faildAction(Exception ex) {
				logger.error(String.format("Batch persist event has unknown exception, the code should not be run to here, errorMessage: {0}", ex.getMessage()), ex);
			}});

	}

	private SystemFutureWrapper<Void> resetCommandMailBoxConsumingSequence(EventCommittingContext context, long consumingSequence) {

		return systemAsyncHelper.submit(() -> {

			EventMailBox eventMailBox = context.eventMailBox;
			ProcessingCommand processingCommand = context.getProcessingCommand();
			ICommand command = processingCommand.getMessage();
			ProcessingCommandMailbox commandMailBox = processingCommand.getMailbox();
			
			DomainEventStream eventStream = context.getEventStream();

			commandMailBox.pause();
			try {
				/// TODO @await
				refreshAggregateMemoryCacheToLatestVersion(eventStream.getAggregateRootTypeName(),
						eventStream.getAggregateRootId()).get();
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
			
		});
		
	}
	
	private void tryToRepublishEventAsync(EventCommittingContext context) {

        ICommand command = context.getProcessingCommand().getMessage();
		
        ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<DomainEventStream>>() {

			@Override
			public String getAsyncActionName() {
				return "FindEventByCommandIdAsync";
			}

			@Override
			public AsyncTaskResult<DomainEventStream> asyncAction() throws Exception {
				return eventStore.findAsync(command.getAggregateRootId(), command.getId()).get();
			}

			@Override
			public void finishAction(AsyncTaskResult<DomainEventStream> result) {

				DomainEventStream existingEventStream = result.getData();
                if (null != existingEventStream)
                {
                    //这里，我们需要再重新做一遍发布事件这个操作；
                    //之所以要这样做是因为虽然该command产生的事件已经持久化成功，但并不表示事件已经发布出去了；
                    //因为有可能事件持久化成功了，但那时正好机器断电了，则发布事件都没有做；
                    publishDomainEventAsync(context.getProcessingCommand(), existingEventStream);
                    
                } else {
                	
                    //到这里，说明当前command想添加到eventStore中时，提示command重复，但是尝试从eventStore中取出该command时却找不到该command。
                    //出现这种情况，我们就无法再做后续处理了，这种错误理论上不会出现，除非eventStore的Add接口和Get接口出现读写不一致的情况；
                    //框架会记录错误日志，让开发者排查具体是什么问题。
                	String errorMessage = String.format("Command should be exist in the event store, but we cannot find it from the event store, this should not be happen, and we cannot continue again. commandType: %s, commandId: %s, aggregateRootId: %s",
                        command.getClass().getName(),
                        command.getId(),
                        command.getAggregateRootId());
                    logger.error(errorMessage);
                    CommandResult commandResult = new CommandResult(CommandStatus.Failed, command.getId(), command.getAggregateRootId(), "Command should be exist in the event store, but we cannot find it from the event store.", String.class.getName());
                    completeCommand(context.getProcessingCommand(), commandResult);
                }
			}

			@Override
			public String getContextInfo() {
				return String.format("[aggregateRootId: %s, commandId: %s]", command.getAggregateRootId(), command.getId());
			}

			@Override
			public void faildAction(Exception ex) {
				logger.error(
						String.format(
								"Find event by commandId has unknown exception, the code should not be run to here, errorMessage: %s",
								ex.getMessage()),
						ex);
			}});
	}

	/**
	 * 遇到Version为1的时间的重复的时候，做特殊处理。
	 * @param context
	 */
	private void handleFirstEventDuplicationAsync(final EventCommittingContext context) {
		
		DomainEventStream eventStream = context.getEventStream();
		
		ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<DomainEventStream>>(){

			@Override
			public String getAsyncActionName() {
				return "FindFirstEventByVersion";
			}

			@Override
			public AsyncTaskResult<DomainEventStream> asyncAction() throws Exception {
				return eventStore.findAsync(eventStream.getAggregateRootId(), 1).get();
			}

			@Override
			public void finishAction(AsyncTaskResult<DomainEventStream> result) {
				
				AsyncTaskResult<DomainEventStream> realResult = (AsyncTaskResult<DomainEventStream> )result;
				DomainEventStream firstEventStream = realResult.getData();
				
				String commandId = context.getProcessingCommand().getMessage().getId();
				
				if(null != firstEventStream) {
					//判断是否是同一个command，如果是，则再重新做一遍发布事件；
                    //之所以要这样做，是因为虽然该command产生的事件已经持久化成功，但并不表示事件也已经发布出去了；
                    //有可能事件持久化成功了，但那时正好机器断电了，则发布事件都没有做；
					if(commandId.equals(firstEventStream.getCommandId())) {
						
						/// TODO ConfigureAwait(false); await
						resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence() + 1)
							.get();
						
						publishDomainEventAsync(context.getProcessingCommand(), firstEventStream);
						
					} else {

                        //如果不是同一个command，则认为是两个不同的command重复创建ID相同的聚合根，我们需要记录错误日志，然后通知当前command的处理完成；
						String errorMessage = String.format("Duplicate aggregate creation. current commandId: %s, existing commandId: %s, aggregateRootId: %s, aggregateRootTypeName: %s",
								commandId,
                            firstEventStream.getCommandId(),
                            firstEventStream.getAggregateRootId(),
                            firstEventStream.getAggregateRootTypeName());
                        logger.error(errorMessage);

						/// TODO ConfigureAwait(false); await
                        resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence() + 1)
                        	.get();
                        
                        CommandResult commandResult = new CommandResult(CommandStatus.Failed, commandId, eventStream.getAggregateRootId(), "Duplicate aggregate creation.", String.class.getName());
                        completeCommand(context.getProcessingCommand(), commandResult);
                        
					}
				} else {
					
					String errorMessage = String.format(
							"Duplicate aggregate creation, but we cannot find the existing eventstream from eventstore, this should not be happen, and we cannot continue again. commandId: %s, aggregateRootId: %s, aggregateRootTypeName: %s",
	                        eventStream.getCommandId(),
	                        eventStream.getAggregateRootId(),
	                        eventStream.getAggregateRootTypeName());
					logger.error(errorMessage);
					
					/// TODO ConfigureAwait(false); await
					resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence() + 1)
						.get();

					CommandResult commandResult = new CommandResult(
							CommandStatus.Failed,
							commandId,
							eventStream.getAggregateRootId(),
							"Duplicate aggregate creation, but we cannot find the existing eventstream from eventstore.",
							String.class.getName());
					completeCommand(context.getProcessingCommand(), commandResult);

				}
			}

			@Override
			public String getContextInfo() {
				return String.format("[eventStream: %s]", jsonSerializer.convert(eventStream));
			}

			@Override
			public void faildAction(Exception ex) {
				logger.error(
					String.format(
						"Find the first version of event has unknown exception, the code should not be run to here, errorMessage: %s",
						ex.getMessage()),
					ex);
			}
			
		});
	}

	private void refreshAggregateMemoryCache(EventCommittingContext context) {
		
		try {
			
			context.getAggregateRoot().acceptChanges(context.getEventStream().getVersion());
			memoryCache.set(context.getAggregateRoot());
			
		} catch (Exception ex) {
			logger.error(
					String.format("Refresh aggregate memory cache failed for event stream:{}", jsonSerializer.convert(context.getEventStream())),
					ex);
		}
		
	}

	private SystemFutureWrapper<Void> refreshAggregateMemoryCacheToLatestVersion(String aggregateRootTypeName, String aggregateRootId) {
		
		try {
			return memoryCache.refreshAggregateFromEventStore(aggregateRootTypeName, aggregateRootId);
		} catch (Exception e) {
            logger.error(String.format(
            		"Refresh aggregate memory cache to latest version has unknown exception, aggregateRootTypeName: %s, aggregateRootId: %s", aggregateRootTypeName, aggregateRootId),
            		e);

    		RipenFuture<Void> rf = new RipenFuture<>();
    		rf.trySetResult(null);
            return new SystemFutureWrapper<Void>(rf);
		}
		
	}

	private void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStreamMessage eventStream) {

		ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResult<Void>>() {

			@Override
			public String getAsyncActionName() {
				return "PublishEventAsync";
			}

			@Override
			public AsyncTaskResult<Void> asyncAction() throws Exception {
				return domainEventPublisher.publishAsync(eventStream).get();
			}

			@Override
			public void finishAction(AsyncTaskResult<Void> result) {
				
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
						String.format("Publish event has unknown exception, the code should not be run to here, errorMessage: %s", ex.getMessage()), ex);
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

	private SystemFutureWrapper<AsyncTaskResult<Void>> completeCommand(ProcessingCommand processingCommand, CommandResult commandResult) {
		return processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
	}

	private void cleanInactiveMailbox() {
		List<String> idelMailboxKeyList = new ArrayList<>();
		ForEachUtil.processForEach(eventMailboxDict, (aggregateId, mailbox) -> {
			if(!mailbox.isRunning() && mailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT))
				idelMailboxKeyList.add(aggregateId);
		});
		
		for(String mailboxKey:idelMailboxKeyList) {
			EventMailBox eventMailBox = eventMailboxDict.get(mailboxKey);
			if(!eventMailBox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT)) {
				// 在上面判断其达到空闲条件后，又被重新使能（临界情况）
				// 放弃本次操作
				continue;
			}
			eventMailboxDict.remove(mailboxKey);
			logger.debug("Removed inactive event mailbox, aggregateRootId: {}", mailboxKey);
		}
	}
}