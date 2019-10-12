package pro.jiefzz.ejoker.commanding.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.util.Collection;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.commanding.CommandResult;
import pro.jiefzz.ejoker.commanding.CommandStatus;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProvider;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import pro.jiefzz.ejoker.commanding.ICommandExecuteContext;
import pro.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import pro.jiefzz.ejoker.commanding.ProcessingCommand;
import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.IMemoryCache;
import pro.jiefzz.ejoker.domain.domainException.IDomainException;
import pro.jiefzz.ejoker.eventing.DomainEventStream;
import pro.jiefzz.ejoker.eventing.EventCommittingContext;
import pro.jiefzz.ejoker.eventing.IDomainEvent;
import pro.jiefzz.ejoker.eventing.IEventCommittingService;
import pro.jiefzz.ejoker.eventing.IEventStore;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.messaging.IApplicationMessage;
import pro.jiefzz.ejoker.messaging.IMessagePublisher;
import pro.jiefzz.ejoker.utils.domainExceptionHelper.DomainExceptionCodecHelper;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureTaskUtil;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.system.task.io.IOHelper;

@EService
public class DefaultProcessingCommandHandler implements IProcessingCommandHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultProcessingCommandHandler.class);

	@Dependence
	private IJSONConverter jsonSerializer;

	@Dependence
	private IEventStore eventStore;
	
	@Dependence
	private ICommandHandlerProvider commandAsyncHandlerPrivider;
	
	@Dependence
	private IEventCommittingService eventCommittingService;

	@Dependence
    private IMessagePublisher<IApplicationMessage> applicationMessagePublisher;

	@Dependence
    private IMessagePublisher<IDomainException> exceptionPublisher;
	
	@Dependence
	private IMemoryCache memoryCache;
	
	@Dependence
	private IOHelper ioHelper;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Override
	public Future<Void> handleAsync(ProcessingCommand processingCommand) {
		ICommand message = processingCommand.getMessage();
		if (StringHelper.isNullOrEmpty(message.getAggregateRootId())) {
			String errorInfo = String.format(
					"The aggregateId of commmand is null or empty! commandType=%s commandId=%s.", message.getClass().getName(),
					message.getId());
			logger.error(errorInfo);
			completeCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorInfo);
		}

		ICommandHandlerProxy asyncHandler = commandAsyncHandlerPrivider.getHandler(message.getClass());
		if(null != asyncHandler) {
			// TODO await
			await(handleCommandInternal(processingCommand, asyncHandler));
		} else {
			String errorMessage = String.format("No command handler found of command. commandType: %s, commandId: %s",
					message.getClass().getName(),
					message.getId());
			logger.error(errorMessage);
			completeCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorMessage);
		}
			
		return EJokerFutureUtil.completeFuture();
	}
	
	private Future<Void> handleCommandInternal(ProcessingCommand processingCommand,
			ICommandHandlerProxy commandHandler) {
		
		ICommand command = processingCommand.getMessage();
		ICommandExecuteContext commandContext = processingCommand.getCommandExecuteContext();
		
		commandContext.clear();
		
		ioHelper.tryAsyncAction2(
				"HandleCommandAsync", 
				() -> {

					/// TODO @await
					await(commandHandler.handleAsync(commandContext, command));
					if(logger.isDebugEnabled()) {
						logger.debug("Handle command success. handlerType:{}, commandType:{}, commandId:{}, aggregateRootId:{}",
	                        commandHandler.getInnerObject().getClass().getName(),
	                        command.getClass().getName(),
	                        command.getId(),
	                        command.getAggregateRootId());
					}
					return EJokerFutureTaskUtil.completeTask();
				},
				() -> {
					if(null != commandContext.getApplicationMessage()) {
						commitChangesAsync(processingCommand, true, commandContext.getApplicationMessage(), null);
					} else {
						try {
							commitAggregateChanges(processingCommand);
						} catch (RuntimeException ex) {
							logger.error(String.format("Commit aggregate changes has unknown exception, handlerType:%s, commandType:%s, commandId:%s, aggregateRootId:%s",
									commandHandler.getInnerObject().getClass().getName(),
			                        command.getClass().getName(),
			                        command.getId(),
			                        command.getAggregateRootId()),
									ex);
							completeCommandAsync(processingCommand, CommandStatus.Failed, ex.getClass().getName(), "Unknown exception caught when committing changes of command.");
						}
					}
				},
				() -> String.format("[command:[id:%s,type:%s],handlerType:%s,aggregateRootId:%s]",
						command.getId(),
						command.getClass().getName(),
						commandHandler.getInnerObject().getClass().getName(),
						command.getAggregateRootId()),
				(e, eMsg) -> {
					handleExceptionAsync(processingCommand, commandHandler, e, eMsg);
				}
			);
		return EJokerFutureUtil.completeFuture();
	}

	// TODO 事件过程的起点
	// 可以在这开始调试EventCommitting过程
	private void commitAggregateChanges(ProcessingCommand processingCommand) {

		ICommand command = processingCommand.getMessage();
		ICommandExecuteContext context = processingCommand.getCommandExecuteContext();
		Collection<IAggregateRoot> trackedAggregateRoots = context.getTrackedAggregateRoots();
		int dirtyAggregateRootCount = 0;
		IAggregateRoot dirtyAggregateRoot = null;
		Collection<IDomainEvent<?>> changeEvents = null;

		for( IAggregateRoot aggregateRoot : trackedAggregateRoots) {
			
			Collection<IDomainEvent<?>> changes = aggregateRoot.getChanges();	
			if(null!=changes && !changes.isEmpty()) {
				dirtyAggregateRootCount++;
				if(dirtyAggregateRootCount>1) {
					String errorInfo = String.format(
							"Detected more than one aggregate created or modified by command!!! commandType=%s commandId=%s",
							command.getClass().getName(),
							command.getId()
					);
					logger.error(errorInfo);
					completeCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorInfo);
					return;
				}
				dirtyAggregateRoot=aggregateRoot;
				changeEvents = changes;
			}
		}

        //如果当前command没有对任何聚合根做修改，框架仍然需要尝试获取该command之前是否有产生事件，
        //如果有，则需要将事件再次发布到MQ；如果没有，则完成命令，返回command的结果为NothingChanged。
        //之所以要这样做是因为有可能当前command上次执行的结果可能是事件持久化完成，但是发布到MQ未完成，然后那时正好机器断电宕机了；
        //这种情况下，如果机器重启，当前command对应的聚合根从eventstore恢复的聚合根是被当前command处理过后的；
        //所以如果该command再次被处理，可能对应的聚合根就不会再产生事件了；
        //所以，我们要考虑到这种情况，尝试再次发布该命令产生的事件到MQ；
        //否则，如果我们直接将当前command设置为完成，即对MQ进行ack操作，那该command的事件就永远不会再发布到MQ了，这样就无法保证CQRS数据的最终一致性了。
		if(0 == dirtyAggregateRootCount || null == changeEvents || changeEvents.isEmpty() ) {
			processIfNoEventsOfCommand(processingCommand);
			return;
		}
		
        //接受聚合根的最新修改
        dirtyAggregateRoot.acceptChanges();

        //刷新聚合根的内存缓存
        // TODO @await
        await(memoryCache.updateAggregateRootCache(dirtyAggregateRoot));

        //构造出一个事件流对象
        String result = processingCommand.getCommandExecuteContext().getResult();
		if(null!=result)
			processingCommand.getItems().put("CommandResult", result);
		
		String aggregateRootTypeName = typeNameProvider.getTypeName(dirtyAggregateRoot.getClass());
		
		DomainEventStream eventStream = new DomainEventStream(
				processingCommand.getMessage().getId(),
				dirtyAggregateRoot.getUniqueId(),
				aggregateRootTypeName,
				System.currentTimeMillis(),
				changeEvents,
				command.getItems()
		);
        //将事件流提交到EventStore
		// TODO event提交从这里开始(可以作为调试点)
		eventCommittingService.commitDomainEventAsync(new EventCommittingContext(dirtyAggregateRoot, eventStream, processingCommand));
		
	}
	
    private void processIfNoEventsOfCommand(ProcessingCommand processingCommand) {
    	ICommand command = processingCommand.getMessage();
    	ioHelper.tryAsyncAction2(
    			"ProcessIfNoEventsOfCommand",
    			() -> eventStore.findAsync(command.getAggregateRootId(), command.getId()),
    			existingEventStream -> {
                    if (null != existingEventStream) {
                        eventCommittingService.publishDomainEventAsync(processingCommand, existingEventStream);
                    } else {
                    	completeCommandAsync(processingCommand, CommandStatus.NothingChanged, String.class.getName(), processingCommand.getCommandExecuteContext().getResult());
                    }
                },
    			() -> String.format("[commandId: %s]", command.getId()),
    			true
    			);
    }
	
	private void handleExceptionAsync(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler, Exception exception, String errorMessage) {
		ICommand command = processingCommand.getMessage();
		ioHelper.tryAsyncAction2(
				"FindEventByCommandIdAsync",
				() -> eventStore.findAsync(command.getAggregateRootId(), command.getId()),
				existingEventStream -> {
					if (existingEventStream != null) {
	                    // 这里，我们需要再重新做一遍发布事件这个操作；
	                    // 之所以要这样做是因为虽然该command产生的事件已经持久化成功，但并不表示事件已经发布出去了；
	                    // 因为有可能事件持久化成功了，但那时正好机器断电了，则发布事件就没有做；
	                    eventCommittingService.publishDomainEventAsync(processingCommand, existingEventStream);
	                
	                } else {
	                	
	                    // 到这里，说明当前command执行遇到异常，然后当前command之前也没执行过，是第一次被执行。
	                    // 那就判断当前异常是否是需要被发布出去的异常，如果是，则发布该异常给所有消费者；
	                    // 否则，就记录错误日志，然后认为该command处理失败即可；
	                	IDomainException publishableException = tryGetDomainException(exception);
	                    if (publishableException != null) {
	                        publishExceptionAsync(processingCommand, publishableException);
	                    } else {
	                        completeCommandAsync(processingCommand, CommandStatus.Failed, exception.getClass().getName(), StringHelper.isNullOrWhiteSpace(errorMessage) ? exception.getMessage() : errorMessage);
	                    }
	                    
	                } },
				() -> String.format("[command:[id:%s,type:%s],handlerType:%s,aggregateRootId:%s]",
						command.getId(),
						command.getClass().getName(),
						commandHandler.getInnerObject().getClass().getName(),
						command.getAggregateRootId()),
				true
				);
	}

    private IDomainException tryGetDomainException(Exception exception) {
        if (exception == null) {
            return null;
        } else if (exception instanceof IDomainException) {
            return (IDomainException )exception;
        }
        // TODO 未完成，找不到AggregateException在哪定义的
        // TODO java里没有聚合异常这个玩意吧？
//        else if (exception is AggregateException)
//        {
//            var aggregateException = exception as AggregateException;
//            var domainException = aggregateException.InnerExceptions.FirstOrDefault(x => x is IDomainException) as IDomainException;
//            return domainException;
//        }
        return null;
    }
	
	
	private void publishExceptionAsync(ProcessingCommand processingCommand, IDomainException exception) {
		exception.mergeItems(processingCommand.getMessage().getItems());
		ioHelper.tryAsyncAction2(
				"PublishExceptionAsync",
				() -> exceptionPublisher.publishAsync(exception),
				r -> completeCommandAsync(processingCommand, CommandStatus.Failed, exception.getClass().getName(), ((Exception )exception).getMessage()),
				() -> String.format("[commandId: %s, exceptionType: %s, exceptionInfo: %s]", processingCommand.getMessage().getId(), exception.getClass().getName(), DomainExceptionCodecHelper.serialize(exception)),
				true
				);
	}
	
	private void commitChangesAsync(ProcessingCommand processingCommand, boolean success, IApplicationMessage message,
			String errorMessage) {
		if (success) {
			if (null != message) {
				message.mergeItems(processingCommand.getMessage().getItems());
				publishMessageAsync(processingCommand, message);
			} else {
				completeCommandAsync(processingCommand, CommandStatus.Success, null, null);
			}
		} else {
			completeCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorMessage);
		}
	}

	private void publishMessageAsync(ProcessingCommand processingCommand, IApplicationMessage message) {
		ICommand command = processingCommand.getMessage();
		
		ioHelper.tryAsyncAction2(
				"PublishApplicationMessageAsync",
				() -> applicationMessagePublisher.publishAsync(message),
				r -> completeCommandAsync(processingCommand, CommandStatus.Success, message.getClass().getName(), jsonSerializer.convert(message)),
				() -> String.format("[application message:[id: %s, type: %s],command:[id: %s, type: %s]]", message.getId(), message.getClass().getName(), command.getId(), command.getClass().getName()),
				ex -> logger.error(String.format("Publish application message has unknown exception, the code should not be run to here, errorMessage: {}", ex.getMessage()), ex),
				true
				);
	}

	private void completeCommandAsync(ProcessingCommand processingCommand,
			CommandStatus commandStatus, String resultType, String result) {
		CommandResult commandResult = new CommandResult(commandStatus, processingCommand.getMessage().getId(),
				processingCommand.getMessage().getAggregateRootId(), result, resultType);
		
        // TODO @await
		await(processingCommand.getMailBox().completeMessage(processingCommand, commandResult));
	}
	
}