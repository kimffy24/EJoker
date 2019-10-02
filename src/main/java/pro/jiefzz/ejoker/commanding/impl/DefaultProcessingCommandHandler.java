package pro.jiefzz.ejoker.commanding.impl;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.commanding.CommandResult;
import pro.jiefzz.ejoker.commanding.CommandRuntimeException;
import pro.jiefzz.ejoker.commanding.CommandStatus;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandAsyncHandlerProvider;
import pro.jiefzz.ejoker.commanding.ICommandAsyncHandlerProxy;
import pro.jiefzz.ejoker.commanding.ICommandExecuteContext;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProvider;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import pro.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import pro.jiefzz.ejoker.commanding.ProcessingCommand;
import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.domain.IMemoryCache;
import pro.jiefzz.ejoker.eventing.DomainEventStream;
import pro.jiefzz.ejoker.eventing.EventCommittingContext;
import pro.jiefzz.ejoker.eventing.IDomainEvent;
import pro.jiefzz.ejoker.eventing.IEventCommittingService;
import pro.jiefzz.ejoker.eventing.IEventStore;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessagePublisher;
import pro.jiefzz.ejoker.infrastructure.messaging.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.infrastructure.messaging.varieties.publishableException.IPublishableException;
import pro.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionCodecHelper;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskStatus;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.system.task.io.IOExceptionOnRuntime;
import pro.jiefzz.ejoker.z.system.task.io.IOHelper;

@EService
public class DefaultProcessingCommandHandler implements IProcessingCommandHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultProcessingCommandHandler.class);

	@Dependence
	private IJSONConverter jsonSerializer;

	@Dependence
	private IEventStore eventStore;
	
	@Dependence
	private ICommandHandlerProvider commandHandlerPrivider;
	
	@Dependence
	private ICommandAsyncHandlerProvider commandAsyncHandlerPrivider;
	
	@Dependence
	private IEventCommittingService eventService;

	@Dependence
    private IMessagePublisher<IApplicationMessage> applicationMessagePublisher;

	@Dependence
    private IMessagePublisher<IPublishableException> exceptionPublisher;
	
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
			return completeCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorInfo);
		}

		try {
			ICommandHandlerProxy handler = commandHandlerPrivider.getHandler(message.getClass());
			if(null != handler) {
				return systemAsyncHelper.submit(() -> handleCommand(processingCommand, handler));
			}
			
			ICommandAsyncHandlerProxy asyncHandler = commandAsyncHandlerPrivider.getHandler(message.getClass());
			if(null != asyncHandler) {
				return handleCommandAsync(processingCommand, asyncHandler);
			}
			
			throw new CommandRuntimeException(message.getClass().getName() +" is no handler found for it!!!");
		} catch (RuntimeException ex) {
			logger.error(ex.getMessage(), ex);
			return completeCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), ex.getMessage());
		}

	}
	
	private void handleCommand(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler) {

		ICommand message = processingCommand.getMessage();

		processingCommand.getCommandExecuteContext().clear();

		//调用command handler执行当前command
		boolean handleSuccess = false;
		try {
			/// TODO @await 直接同步实现
			/// 此处由本类的handle()方法通过异步的方式发起调用
			commandHandler.handle(processingCommand.getCommandExecuteContext(), message);
			logger.debug("Handle command success. [handlerType={}, commandType={}, commandId={}, aggregateRootId={}]",
					commandHandler.toString(), message.getClass().getName(), message.getId(), message.getAggregateRootId());
			handleSuccess = true;
		} catch (Exception ex) {
			handleExceptionAsync(processingCommand, commandHandler, ex);
			return;
		}

		//如果command执行成功，则提交执行后的结果
		if (handleSuccess) {
			try {
				// TODO 事件过程的起点
				commitAggregateChanges(processingCommand);
			} catch (RuntimeException ex) {
				logCommandExecuteException(processingCommand, commandHandler, ex);
				// TODO @await
				await( completeCommandAsync(processingCommand, CommandStatus.Failed, ex.getClass().getName(),
						"Unknow exception caught when committing changes of command.") );
			}
		}

	}

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
        memoryCache.updateAggregateRootCache(dirtyAggregateRoot);

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
		eventService.commitDomainEventAsync(new EventCommittingContext(dirtyAggregateRoot, eventStream, processingCommand));
		
	}
	
    private void processIfNoEventsOfCommand(ProcessingCommand processingCommand) {
    	ICommand command = processingCommand.getMessage();
    	ioHelper.tryAsyncAction2(
    			"ProcessIfNoEventsOfCommand",
    			() -> eventStore.findAsync(command.getAggregateRootId(), command.getId()),
    			existingEventStream -> {
                    if (null != existingEventStream) {
                        eventService.publishDomainEventAsync(processingCommand, existingEventStream);
                    } else {
                        completeCommandAsync(processingCommand, CommandStatus.NothingChanged, String.class.getName(), processingCommand.getCommandExecuteContext().getResult());
                    } },
    			() -> String.format("[commandId: %s]", command.getId()),
    			ex -> logger.error(
						String.format(
								"Find event by commandId has unknown exception, the code should not be run to here, errorMessage: {}",
								ex.getMessage()),
						ex),
    			true
    			);
    }
	
	private void handleExceptionAsync(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler, Exception exception) {
		ICommand command = processingCommand.getMessage();
		ioHelper.tryAsyncAction2(
				"FindEventByCommandIdAsync",
				() -> eventStore.findAsync(command.getAggregateRootId(), command.getId()),
				existingEventStream -> {
					if (existingEventStream != null) {
	                    // 这里，我们需要再重新做一遍发布事件这个操作；
	                    // 之所以要这样做是因为虽然该command产生的事件已经持久化成功，但并不表示事件已经发布出去了；
	                    // 因为有可能事件持久化成功了，但那时正好机器断电了，则发布事件就没有做；
	                    eventService.publishDomainEventAsync(processingCommand, existingEventStream);
	                
	                } else {
	                	
	                    // 到这里，说明当前command执行遇到异常，然后当前command之前也没执行过，是第一次被执行。
	                    // 那就判断当前异常是否是需要被发布出去的异常，如果是，则发布该异常给所有消费者；
	                    // 否则，就记录错误日志，然后认为该command处理失败即可；
	                	IPublishableException publishableException
	                		= (exception instanceof IPublishableException) ? (IPublishableException )exception : null;
	                    if (publishableException != null) {
	                        publishExceptionAsync(processingCommand, publishableException);
	                    } else {
	                        logCommandExecuteException(processingCommand, commandHandler, exception);
	                        completeCommandAsync(processingCommand, CommandStatus.Failed, exception.getClass().getName(), exception.getMessage());
	                    }
	                    
	                } },
				() -> String.format("[commandId: %s]", command.getId()),
				ex -> logger.error(
						String.format(
								"Find event by commandId has unknown exception, the code should not be run to here, errorMessage: {}",
								ex.getMessage()),
						ex),
				true
				);
	}
	
	
	private void publishExceptionAsync(ProcessingCommand processingCommand, IPublishableException exception) {
		exception.mergeItems(processingCommand.getMessage().getItems());
		ioHelper.tryAsyncAction2(
				"PublishExceptionAsync",
				() -> exceptionPublisher.publishAsync(exception),
				r -> completeCommandAsync(processingCommand, CommandStatus.Failed, exception.getClass().getName(), ((Exception )exception).getMessage()),
				() -> String.format("[commandId: %s, exceptionType: %s, exceptionInfo: %s]", processingCommand.getMessage().getId(), exception.getClass().getName(), PublishableExceptionCodecHelper.serialize(exception)),
				ex -> logger.error(String.format("Publish event has unknown exception, the code should not be run to here, errorMessage: %s", ex.getMessage()), ex),
				true
				);
	}

    private void logCommandExecuteException(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler, Exception exception) {
    	ICommand command = processingCommand.getMessage();
    	String errorMessage = String.format("%s raised when %s handling %s. commandId: %s, aggregateRootId: %s",
            exception.getClass().getName(),
            commandHandler.toString(),
            command.getClass().getName(),
            command.getId(),
            command.getAggregateRootId());
        logger.error(errorMessage, exception);
    }
	
	private Future<Void> handleCommandAsync(ProcessingCommand processingCommand, ICommandAsyncHandlerProxy commandHandler) {
		ICommand command = processingCommand.getMessage();
		
		ioHelper.tryAsyncAction2(
					"HandleCommandAsync",
					() ->  {
						try {
							Future<AsyncTaskResult<IApplicationMessage>> ressult = commandHandler.handleAsync(processingCommand.getCommandExecuteContext(), command);
							logger.debug("Handle command async success. handler:{}, commandType:{}, commandId:{}, aggregateRootId:{}",
		                            commandHandler.toString(),
		                            command.getClass().getName(),
		                            command.getId(),
		                            command.getAggregateRootId());
							
							return ressult;
						} catch (Exception ex) {
							
							while(ex instanceof IOExceptionOnRuntime)
								ex = (Exception )ex.getCause();
							
							boolean isIOException = ex instanceof IOException;
							
		                    logger.error(String.format("Handle command async has%s exception occur. handler:%s, commandType:%s, commandId:%s, aggregateRootId:%s",
		                    		(isIOException ?  " io" : ""),
		                    		commandHandler.toString(),
		                            command.getClass().getName(),
		                            command.getId(),
		                            command.getAggregateRootId()), ex);

		            		return EJokerFutureUtil.completeFuture(new AsyncTaskResult<>(isIOException ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed, ex.getMessage()));
		                }
					},
					r -> commitChangesAsync(processingCommand, true, r, null),
					() -> String.format("[command: [id: %s, type: %s], handler: %s]", command.getId(), command.getClass().getName(), commandHandler.toString()),
					ex -> commitChangesAsync(processingCommand, false, null, ex.getMessage())
		);
		return EJokerFutureUtil.completeFuture();
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

	private Future<Void> completeCommandAsync(ProcessingCommand processingCommand,
			CommandStatus commandStatus, String resultType, String result) {
		CommandResult commandResult = new CommandResult(commandStatus, processingCommand.getMessage().getId(),
				processingCommand.getMessage().getAggregateRootId(), result, resultType);
		// Future传递
		return processingCommand.getMailBox().completeMessage(processingCommand, commandResult);
	}
	
}