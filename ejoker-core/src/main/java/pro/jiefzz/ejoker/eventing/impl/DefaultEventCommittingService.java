package pro.jiefzz.ejoker.eventing.impl;

import static pro.jiefzz.ejoker.common.system.extension.LangUtil.await;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.commanding.CommandResult;
import pro.jiefzz.ejoker.commanding.CommandStatus;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ProcessingCommand;
import pro.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.common.context.annotation.context.EService;
import pro.jiefzz.ejoker.common.service.IJSONConverter;
import pro.jiefzz.ejoker.common.system.enhance.ForEachUtil;
import pro.jiefzz.ejoker.common.system.enhance.MapUtil;
import pro.jiefzz.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.common.system.task.io.IOHelper;
import pro.jiefzz.ejoker.domain.IAggregateRootFactory;
import pro.jiefzz.ejoker.domain.IAggregateStorage;
import pro.jiefzz.ejoker.domain.IMemoryCache;
import pro.jiefzz.ejoker.eventing.DomainEventStream;
import pro.jiefzz.ejoker.eventing.DomainEventStreamMessage;
import pro.jiefzz.ejoker.eventing.EventCommittingContext;
import pro.jiefzz.ejoker.eventing.EventCommittingContextMailBox;
import pro.jiefzz.ejoker.eventing.IEventCommittingService;
import pro.jiefzz.ejoker.eventing.IEventStore;
import pro.jiefzz.ejoker.messaging.IMessagePublisher;

@EService
public class DefaultEventCommittingService implements IEventCommittingService {

	private final static Logger logger = LoggerFactory.getLogger(DefaultEventCommittingService.class);

	private final int eventMailboxCount = EJokerEnvironment.EVENT_MAILBOX_ACTOR_COUNT;
	
	@Dependence
	private IJSONConverter jsonSerializer;
	
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
	
	private final List<EventCommittingContextMailBox> eventCommittingContextMailBoxList = new ArrayList<>();

	@EInitialize
	private void init() {
		
		for(int i=0; i<eventMailboxCount; i++) {
			eventCommittingContextMailBoxList.add(
					new EventCommittingContextMailBox(i, EJokerEnvironment.MAX_BATCH_EVENTS, this::batchPersistEventCommittingContexts, systemAsyncHelper));
		}
		
	}
	
	@Override
	public void commitDomainEventAsync(EventCommittingContext context) {
		
		int eventMailBoxIndex = getEventMailBoxIndex(context.getEventStream().getAggregateRootId());
		EventCommittingContextMailBox eventMailBox = eventCommittingContextMailBoxList.get(eventMailBoxIndex);
		eventMailBox.enqueueMessage(context);

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
		if (null == eventStream.getItems() || eventStream.getItems().isEmpty())
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

	// ================ //
	// Private Methods
	
	private int getEventMailBoxIndex(String aggregateRootId) {
		int hash = 23;
		for(byte c : aggregateRootId.getBytes()) {
			hash = (hash << 5) - hash + c;
		}
		if(hash < 0) {
			hash = Math.abs(hash);
		}
		return hash % eventMailboxCount;
	}

	private void batchPersistEventCommittingContexts(List<EventCommittingContext> committingContexts) {
		
		if (null == committingContexts || committingContexts.isEmpty())
			return;
		
		batchPersistEventAsync(committingContexts);

	}
	
	private void batchPersistEventAsync(List<EventCommittingContext> committingContexts) {
		
		LinkedList<DomainEventStream> domainEventStreams = new LinkedList<>();
		ForEachUtil.processForEach(committingContexts, item -> domainEventStreams.add(item.getEventStream()));
		
		ioHelper.tryAsyncAction2(
				"BatchPersistEventAsync",
				() -> eventStore.batchAppendAsync(domainEventStreams),
				appendResult -> {

					EventCommittingContext firstEventCommittingContext = committingContexts.get(0);
					EventCommittingContextMailBox eventMailBox = firstEventCommittingContext.getMailBox();
					
					List<String> successIds = appendResult.getSuccessAggregateRootIdList();
					if(null != successIds && !successIds.isEmpty()) {
						// 针对持久化成功的聚合根，发布这些聚合根的事件到Q端
						Map<String, List<EventCommittingContext>> successCommittedContextDict = new HashMap<>();
						
						// 这个位置用java stream操作不太顺手
				        for(EventCommittingContext ecc : committingContexts) {
				        	MapUtil
				        		.getOrAdd(successCommittedContextDict, ecc.getEventStream().getAggregateRootId(), () -> new ArrayList<>())
				        		.add(ecc);
				        }
				        
				        if(logger.isDebugEnabled()) {
				        	StringBuilder sb = new StringBuilder();
				        	
				        	sb.append('{');
				        	ForEachUtil.processForEach(successCommittedContextDict, (aggrId, contexts) -> {
				        		sb.append('"');
				        		sb.append(aggrId);
				        		sb.append("\": [");
				        		for(EventCommittingContext ecc : contexts) {
				        			// TODO 这个地方遇上泛型就扑街了。
				        			sb.append(jsonSerializer.convert(ecc));
				        			sb.append(", ");
				        		}
					        	sb.deleteCharAt(sb.length()-1);
					        	sb.deleteCharAt(sb.length()-1);
				        		sb.append("], ");
				        	});
				        	sb.deleteCharAt(sb.length()-1);
				        	sb.deleteCharAt(sb.length()-1);
				        	sb.append('}');
				        	
				        	logger.debug(
				        			"Batch persist events, mailboxNumber: {}, succeedAggregateRootCount: {}, eventStreamDetail: {}",
				        			eventMailBox.getNumber(),
				        			successIds.size(),
				        			sb.toString()
				        			);
				        }
				        
				        systemAsyncHelper.submit(() -> {
				        	ForEachUtil.processForEach(successCommittedContextDict, (aggrId, contexts) -> {
				        		for(EventCommittingContext ecc : contexts) {
				        			publishDomainEventAsync(ecc.getProcessingCommand(), ecc.getEventStream());
				        		}
				        	});
				        });
					}
					
					if(null != appendResult.getDuplicateCommandIdList() && !appendResult.getDuplicateCommandIdList().isEmpty()) {
		                //针对持久化出现重复的命令ID，则重新发布这些命令对应的领域事件到Q端
						List<String> duplicateCommandIdList = appendResult.getDuplicateCommandIdList();
						
						logger.warn("Batch persist events, mailboxNumber: {}, duplicateCommandIdCount: {}, detail: {}",
		                        eventMailBox.getNumber(),
		                        duplicateCommandIdList.size(),
		                        duplicateCommandIdList.toString());
						
						for(String commandId : duplicateCommandIdList) {
							Optional<EventCommittingContext> optional = committingContexts.stream().filter(x -> commandId.equals(x.getProcessingCommand().getMessage().getId())).findFirst();
							if(optional.isPresent()) {
								EventCommittingContext eventCommittingContext = optional.get();
								resetCommandMailBoxConsumingSequence(eventCommittingContext, eventCommittingContext.getProcessingCommand().getSequence() + 1)
									.thenAcceptAsync(t -> {
										tryToRepublishEventAsync(eventCommittingContext);
									});
							}
						}
					}
					
					if(null != appendResult.getDuplicateEventAggregateRootIdList() && 0 > appendResult.getDuplicateEventAggregateRootIdList().size()) {
		                //针对持久化出现版本冲突的聚合根，则自动处理每个聚合根的冲突
						List<String> duplicateAggrIdList = appendResult.getDuplicateEventAggregateRootIdList();
						
						logger.warn("Batch persist events, mailboxNumber: {}, duplicateEventAggregateRootCount: {}, detail: {}",
		                        eventMailBox.getNumber(),
		                        duplicateAggrIdList.size(),
		                        duplicateAggrIdList.toString());

						for(String commandId : duplicateAggrIdList) {
							Optional<EventCommittingContext> optional = committingContexts.stream().filter(x -> commandId.equals(x.getProcessingCommand().getMessage().getId())).findFirst();
							EventCommittingContext eventCommittingContext = optional.get();
							if(null != eventCommittingContext) {
								processAggregateDuplicateEvent(eventCommittingContext);
							}
						}
					}
					
					eventMailBox.finishRun();
					
				},
				() -> String.format("[contextListCount: %d]", committingContexts.size()),
				true
			);
	}
	
	private void processAggregateDuplicateEvent(EventCommittingContext eventCommittingContext) {
		if(1l == eventCommittingContext.getEventStream().getVersion()) {
			handleFirstEventDuplicationAsync(eventCommittingContext);
		} else {
			resetCommandMailBoxConsumingSequence(eventCommittingContext, eventCommittingContext.getProcessingCommand().getSequence())
			.thenRunAsync(() -> {});
		}
	}

	/**
	 * * 这里是由CompletableFuture提供的任务链功能
	 * @param context
	 * @param consumingSequence
	 * @return
	 */
	private CompletableFuture<Void> resetCommandMailBoxConsumingSequence(EventCommittingContext context, long consumingSequence) {

//		final EventMailBox eventMailBox = context.eventMailBox;
//		IMailBox<EventCommittingContext, Void> eventMailBox = context.getMailBox();
		final ProcessingCommand processingCommand = context.getProcessingCommand();
		final ICommand command = processingCommand.getMessage();
		final ProcessingCommandMailbox commandMailBox = processingCommand.getMailBox();
		final EventCommittingContextMailBox eventMailBox = context.getMailBox();
		final String aggregateRootId = context.getEventStream().getAggregateRootId();
		
		// 设置暂停标识，不排斥当前运行的任务，但是拒绝新的任务进入
		commandMailBox.pauseOnly();

		// commandMailBox.pause() 与 commandMailBox.resume() 并不在成对的try-finally过程中
		// 会不会出问题？
		// #fix 把pause过程拆解为pauseOnly 和 waitAcquireOnProcessing两个过程 更符合java
		return CompletableFuture.supplyAsync(() -> {

			// 等待完全的pause状态
			commandMailBox.acquireOnRunning();
			
			try {
				
				eventMailBox.removeAggregateAllEventCommittingContexts(aggregateRootId);
				// TODO @await
				await(memoryCache.refreshAggregateFromEventStoreAsync(context.getEventStream().getAggregateRootTypeName(), aggregateRootId));
				commandMailBox.resetConsumingSequence(consumingSequence);
				
			} catch (RuntimeException ex) {
				logger.error(String.format(
						"ResetCommandMailBoxConsumingOffset has unknown exception, aggregateRootId: %s",
						command.getAggregateRootId()), ex);
			} finally {
				commandMailBox.resume();
				commandMailBox.tryRun();
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
                        finishCommandAsync(context.getProcessingCommand(), commandResult);
                        
                    }
        		},
        		() -> String.format("[aggregateRootId: %s, commandId: %s]", command.getAggregateRootId(), command.getId()),
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
    						
							resetCommandMailBoxConsumingSequence(context,
									context.getProcessingCommand().getSequence() + 1).thenAcceptAsync(t -> {
										publishDomainEventAsync(context.getProcessingCommand(), firstEventStream);
									});
    						
    					} else {

                            //如果不是同一个command，则认为是两个不同的command重复创建ID相同的聚合根，我们需要记录错误日志，然后通知当前command的处理完成；
    						String errorMessage = String.format("Duplicate aggregate creation. current commandId: %s, existing commandId: %s, aggregateRootId: %s, aggregateRootTypeName: %s",
    								commandId,
                                firstEventStream.getCommandId(),
                                firstEventStream.getAggregateRootId(),
                                firstEventStream.getAggregateRootTypeName());
							logger.error(errorMessage);

							resetCommandMailBoxConsumingSequence(context,
									context.getProcessingCommand().getSequence() + 1).thenAcceptAsync(t -> {
										CommandResult commandResult = new CommandResult(CommandStatus.Failed, commandId,
												eventStream.getAggregateRootId(), "Duplicate aggregate creation.",
												String.class.getName());
										finishCommandAsync(context.getProcessingCommand(), commandResult);
									});
                            
    					}
    				} else {
    					
    					String errorMessage = String.format(
    							"Duplicate aggregate creation, but we cannot find the existing eventstream from eventstore, this should not be happen, and we cannot continue again. commandId: %s, aggregateRootId: %s, aggregateRootTypeName: %s",
    	                        eventStream.getCommandId(),
    	                        eventStream.getAggregateRootId(),
    	                        eventStream.getAggregateRootTypeName());
						logger.error(errorMessage);

						resetCommandMailBoxConsumingSequence(context, context.getProcessingCommand().getSequence() + 1)
								.thenApplyAsync(t -> {
									CommandResult commandResult = new CommandResult(CommandStatus.Failed, commandId,
											eventStream.getAggregateRootId(),
											"Duplicate aggregate creation, but we cannot find the existing eventstream from eventstore.",
											String.class.getName());
									finishCommandAsync(context.getProcessingCommand(), commandResult);
									return null;
								});
    				}
        		},
        		() -> String.format("[eventStream: %s]", jsonSerializer.convert(eventStream)),
        		true
        		);
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
					finishCommandAsync(processingCommand, commandResult);
					},
				() -> String.format("[eventStream: %s]", eventStream.toString()),
				true
				);
	}

	private Future<Void> finishCommandAsync(ProcessingCommand processingCommand, CommandResult commandResult) {
		return processingCommand.getMailBox().finishMessage(processingCommand, commandResult);
	}

}