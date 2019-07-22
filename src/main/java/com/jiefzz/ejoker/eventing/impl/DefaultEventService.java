package com.jiefzz.ejoker.eventing.impl;

import static com.jiefzz.ejoker.z.common.system.extension.LangUtil.await;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
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
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.helper.ForEachHelper;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

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
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;

	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				this::cleanInactiveMailbox,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT);
	}

	@Override
	public void commitDomainEventAsync(EventCommittingContext context) {
		String uniqueId = context.getAggregateRoot().getUniqueId();
		EventMailBox eventMailbox = MapHelper.getOrAddConcurrent(
				eventMailboxDict,
				uniqueId,
				() -> new EventMailBox(
						uniqueId,
						committingContexts -> {
							if (committingContexts == null || committingContexts.size() == 0)
								return;
							if (eventStore.isSupportBatchAppendEvent())
								batchPersistEventAsync(committingContexts);
							else
								persistEventOneByOne(committingContexts);
						},
						systemAsyncHelper));
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


	private void batchPersistEventAsync(List<EventCommittingContext> committingContexts) {

		LinkedHashSet<DomainEventStream> domainEventStreams = new LinkedHashSet<>();
		ForEachHelper.processForEach(committingContexts, item -> domainEventStreams.add(item.getEventStream()));
		
		ioHelper.tryAsyncAction2(
				"BatchPersistEventAsync",
				() -> eventStore.batchAppendAsync(domainEventStreams),
				appendResult -> {

					EventCommittingContext firstEventCommittingContext = committingContexts.get(0);
					EventMailBox eventMailBox = firstEventCommittingContext.eventMailBox;
					
					if(EventAppendResult.Success.equals(appendResult)) {
						
						logger.debug(
								"Batch persist event success, aggregateRootId: {}, eventStreamCount: {}",
								eventMailBox.getAggregateRootId(),
								committingContexts.size()
						);
						
						systemAsyncHelper.submit(() -> {
								ForEachHelper.processForEach(committingContexts,
										context -> publishDomainEventAsync(context.getProcessingCommand(), context.getEventStream()));
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
	                        /// TODO .ConfigureAwait(false);
	    					resetCommandMailBoxConsumingSequence(
		                        		firstEventCommittingContext,
		                        		firstEventCommittingContext.getProcessingCommand().getSequence());
	                        
						}
						
					} else if (EventAppendResult.DuplicateCommand.equals(appendResult)) {
						
	                    persistEventOneByOne(committingContexts);
	                    
	                } },
				() -> String.format("[contextListCount: %d]", committingContexts.size()),
				ex -> logger.error(String.format("Batch persist event has unknown exception, the code should not be run to here, errorMessage: %s", ex.getMessage()), ex),
				true
				);
	}
	
	private void persistEventOneByOne(List<EventCommittingContext> contextList) {
		// 逐个持久化
		concatContexts(contextList);
		persistEventAsync(contextList.get(0));

	}

	private void persistEventAsync(final EventCommittingContext context) {
		ioHelper.tryAsyncAction2(
				"PersistEventAsync",
				() -> eventStore.appendAsync(context.getEventStream()),
				realrResult -> {
					switch (realrResult) {
						case Success:
							logger.debug("Persist event success, {}", jsonSerializer.convert(context.getEventStream()));
							
							systemAsyncHelper.submit(
									() -> publishDomainEventAsync(context.getProcessingCommand(), context.getEventStream())
							);
							
							if (null != context.next)
								persistEventAsync(context.next);
							else
								context.eventMailBox.tryRun(true);
							
							break;
							
						case DuplicateEvent:
							if(context.getEventStream().getVersion() - 1l == 0l) {
								handleFirstEventDuplicationAsync(context);
							} else {
								logger.warn("Persist event has concurrent version conflict, eventStream: {}", jsonSerializer.convert(context.getEventStream()));
								
								/// TODO .ConfigureAwait(false);
								resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence());
							}
							break;
							
						case DuplicateCommand:
		                    logger.warn("Persist event has duplicate command, eventStream: {}", jsonSerializer.convert(context.getEventStream()));
		
							/// TODO .ConfigureAwait(false);
		                    resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence() + 1l);
		                    tryToRepublishEventAsync(context);
							break;
							
						default:
							assert false;
						}
					},
				() -> String.format("[eventStream: %s]", jsonSerializer.convert(context.getEventStream())),
				ex -> logger.error(String.format("Batch persist event has unknown exception, the code should not be run to here, errorMessage: %s", ex.getMessage()), ex),
				true
				);

	}

	/**
	 * * 这里是由CompletableFuture提供的任务链功能
	 * @param context
	 * @param consumingSequence
	 * @return
	 */
	private CompletableFuture<Void> resetCommandMailBoxConsumingSequence(EventCommittingContext context, long consumingSequence) {

		final EventMailBox eventMailBox = context.eventMailBox;
		final ProcessingCommand processingCommand = context.getProcessingCommand();
		final ICommand command = processingCommand.getMessage();
		final ProcessingCommandMailbox commandMailBox = processingCommand.getMailbox();
		
		commandMailBox.pauseOnly();

		// commandMailBox.pause() 与 commandMailBox.resume() 并不在成对的try-finally过程中
		// 会不会出问题？
		// #fix 把pause过程拆解为pauseOnly 和 waitAcquireOnProcessing两个过程 更符合java
		return CompletableFuture.supplyAsync(() -> {

//			commandMailBox.pause();
			commandMailBox.waitAcquireOnProcessing();

			DomainEventStream eventStream = context.getEventStream();
			try {
				// TODO @await
				await(refreshAggregateMemoryCacheToLatestVersionAsync(eventStream.getAggregateRootTypeName(), eventStream.getAggregateRootId()));
				commandMailBox.resetConsumingSequence(consumingSequence);
				eventMailBox.clear();
				eventMailBox.exit();
				logger.debug(
						"ResetCommandMailBoxConsumingSequence success, commandId: {}, aggregateRootId: {}, consumingSequence: {}",
						command.getId(), command.getAggregateRootId(), consumingSequence);
			} catch (RuntimeException ex) {
				logger.error(String.format(
						"ResetCommandMailBoxConsumingOffset has unknown exception, commandId: %s, aggregateRootId: %s",
						command.getId(), command.getAggregateRootId()), ex);
			} finally {
				commandMailBox.resume();
			}
			return null;
		});

	}
	
	private void tryToRepublishEventAsync(EventCommittingContext context) {

        ICommand command = context.getProcessingCommand().getMessage();
		
        ioHelper.tryAsyncAction2(
        		"FindEventByCommandIdAsync",
        		() -> eventStore.findAsync(command.getAggregateRootId(), command.getId()),
        		existingEventStream -> {
        			if (null != existingEventStream) {
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
                        completeCommandAsync(context.getProcessingCommand(), commandResult);
                    } },
        		() -> String.format("[aggregateRootId: %s, commandId: %s]", command.getAggregateRootId(), command.getId()),
        		ex -> logger.error(
						String.format(
								"Find event by commandId has unknown exception, the code should not be run to here, errorMessage: %s",
								ex.getMessage()),
						ex),
        		true
        		);
	}

	/**
	 * 遇到Version为1的事件的重复的时候，做特殊处理。
	 * @param context
	 */
	private void handleFirstEventDuplicationAsync(final EventCommittingContext context) {
		
		DomainEventStream eventStream = context.getEventStream();

        ioHelper.tryAsyncAction2(
        		"FindFirstEventByVersion",
        		() -> eventStore.findAsync(eventStream.getAggregateRootId(), 1),
        		firstEventStream -> {

    				String commandId = context.getProcessingCommand().getMessage().getId();
    				if(null != firstEventStream) {
    					//判断是否是同一个command，如果是，则再重新做一遍发布事件；
                        //之所以要这样做，是因为虽然该command产生的事件已经持久化成功，但并不表示事件也已经发布出去了；
                        //有可能事件持久化成功了，但那时正好机器断电了，则发布事件都没有做；
    					if(commandId.equals(firstEventStream.getCommandId())) {
    						
							/// TODO .ConfigureAwait(false);
							resetCommandMailBoxConsumingSequence(context,
									context.getProcessingCommand().getSequence() + 1).thenApplyAsync(t -> {
										publishDomainEventAsync(context.getProcessingCommand(), firstEventStream);
										return null;
									});
    						
    					} else {

                            //如果不是同一个command，则认为是两个不同的command重复创建ID相同的聚合根，我们需要记录错误日志，然后通知当前command的处理完成；
    						String errorMessage = String.format("Duplicate aggregate creation. current commandId: %s, existing commandId: %s, aggregateRootId: %s, aggregateRootTypeName: %s",
    								commandId,
                                firstEventStream.getCommandId(),
                                firstEventStream.getAggregateRootId(),
                                firstEventStream.getAggregateRootTypeName());
							logger.error(errorMessage);

							/// TODO .ConfigureAwait(false);
							resetCommandMailBoxConsumingSequence(context,
									context.getProcessingCommand().getSequence() + 1).thenApplyAsync(t -> {
										CommandResult commandResult = new CommandResult(CommandStatus.Failed, commandId,
												eventStream.getAggregateRootId(), "Duplicate aggregate creation.",
												String.class.getName());
										completeCommandAsync(context.getProcessingCommand(), commandResult);
										return null;
									});
                            
    					}
    				} else {
    					
    					String errorMessage = String.format(
    							"Duplicate aggregate creation, but we cannot find the existing eventstream from eventstore, this should not be happen, and we cannot continue again. commandId: %s, aggregateRootId: %s, aggregateRootTypeName: %s",
    	                        eventStream.getCommandId(),
    	                        eventStream.getAggregateRootId(),
    	                        eventStream.getAggregateRootTypeName());
						logger.error(errorMessage);

						/// TODO .ConfigureAwait(false);
						resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence() + 1)
								.thenApplyAsync(t -> {
									CommandResult commandResult = new CommandResult(CommandStatus.Failed, commandId,
											eventStream.getAggregateRootId(),
											"Duplicate aggregate creation, but we cannot find the existing eventstream from eventstore.",
											String.class.getName());
									completeCommandAsync(context.getProcessingCommand(), commandResult);
									return null;
								});
    				}
        		},
        		() -> String.format("[eventStream: %s]", jsonSerializer.convert(eventStream)),
        		ex -> logger.error(
    					String.format(
    							"Find the first version of event has unknown exception, the code should not be run to here, errorMessage: %s",
    							ex.getMessage()),
    						ex),
        		true
        		);
	}

	private void refreshAggregateMemoryCache(EventCommittingContext context) {
		
		try {
			
			context.getAggregateRoot().acceptChanges(context.getEventStream().getVersion());
			memoryCache.set(context.getAggregateRoot());
			
		} catch (RuntimeException ex) {
			logger.error(
					String.format("Refresh aggregate memory cache failed for event stream:{}", jsonSerializer.convert(context.getEventStream())),
					ex);
		}
		
	}

	private SystemFutureWrapper<Void> refreshAggregateMemoryCacheToLatestVersionAsync(String aggregateRootTypeName, String aggregateRootId) {
		try {
			return memoryCache.refreshAggregateFromEventStoreAsync(aggregateRootTypeName, aggregateRootId);
		} catch (RuntimeException e) {
            logger.error(String.format(
            		"Refresh aggregate memory cache to latest version has unknown exception, aggregateRootTypeName: %s, aggregateRootId: %s",
            		aggregateRootTypeName,
            		aggregateRootId),
            	e);
            return SystemFutureWrapperUtil.completeFuture();
		}
		
	}

	private void publishDomainEventAsync(ProcessingCommand processingCommand, DomainEventStreamMessage eventStream) {

		ioHelper.tryAsyncAction2(
				"PublishEventAsync",
				() -> domainEventPublisher.publishAsync(eventStream),
				r -> {
					
					logger.debug("Publish event success, {}", eventStream.toString());

					String commandHandleResult = processingCommand.getCommandExecuteContext().getResult();
					CommandResult commandResult = new CommandResult(
							CommandStatus.Success,
							processingCommand.getMessage().getId(),
							eventStream.getAggregateRootId(),
							commandHandleResult,
							String.class.getName());
					completeCommandAsync(processingCommand, commandResult);
					},
				() -> String.format("[eventStream: %s]", eventStream.toString()),
				ex -> logger.error(
						String.format(
								"Publish event has unknown exception, the code should not be run to here, errorMessage: %s",
								ex.getMessage()),
						ex),
				true
				);
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

	private SystemFutureWrapper<Void> completeCommandAsync(ProcessingCommand processingCommand, CommandResult commandResult) {
		return processingCommand.getMailbox().completeMessageAsync(processingCommand, commandResult);
	}

	private void cleanInactiveMailbox() {
		Iterator<Entry<String, EventMailBox>> it = eventMailboxDict.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, EventMailBox> current = it.next();
			EventMailBox mailbox = current.getValue();
			if(!mailbox.isRunning() && mailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT)) {
				it.remove();
				logger.debug("Removed inactive event mailbox, aggregateRootId: {}", current.getKey());
			}
		}
	}
}