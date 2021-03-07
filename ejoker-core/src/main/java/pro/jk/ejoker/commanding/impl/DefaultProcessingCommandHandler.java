package pro.jk.ejoker.commanding.impl;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.commanding.CommandResult;
import pro.jk.ejoker.commanding.CommandStatus;
import pro.jk.ejoker.commanding.ICommand;
import pro.jk.ejoker.commanding.ICommandExecuteContext;
import pro.jk.ejoker.commanding.ICommandHandlerProvider;
import pro.jk.ejoker.commanding.ICommandHandlerProxy;
import pro.jk.ejoker.commanding.IProcessingCommandHandler;
import pro.jk.ejoker.commanding.ProcessingCommand;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.common.system.enhance.StringUtilx;
import pro.jk.ejoker.common.system.extension.AsyncWrapperException;
import pro.jk.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.common.system.task.io.IOHelper;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.IMemoryCache;
import pro.jk.ejoker.domain.domainException.IDomainException;
import pro.jk.ejoker.eventing.DomainEventStream;
import pro.jk.ejoker.eventing.EventCommittingContext;
import pro.jk.ejoker.eventing.IDomainEvent;
import pro.jk.ejoker.eventing.IEventCommittingService;
import pro.jk.ejoker.eventing.IEventStore;
import pro.jk.ejoker.infrastructure.ITypeNameProvider;
import pro.jk.ejoker.messaging.IApplicationMessage;
import pro.jk.ejoker.messaging.IMessagePublisher;
import pro.jk.ejoker.utils.domainExceptionHelper.DomainExceptionCodecHelper;

@EService
public class DefaultProcessingCommandHandler implements IProcessingCommandHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultProcessingCommandHandler.class);

	@Dependence
	private IJSONStringConverterPro jsonSerializer;

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
		if (StringUtilx.isNullOrEmpty(message.getAggregateRootId())) {
			String errorInfo = StringUtilx.fmt("The aggregateId of commmand is null or empty! [commandType: {} commandId: {}]",
					message.getClass().getName(),
					message.getId());
			logger.error(errorInfo);
			finishCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorInfo);
		}

		ICommandHandlerProxy asyncHandler = commandAsyncHandlerPrivider.getHandler(message.getClass());
		if(null != asyncHandler) {
			// TODO @await
			await(handleCommandInternal(processingCommand, asyncHandler));
		} else {
			String errorMessage = StringUtilx.fmt("No command handler found of command! [commandType: {}, commandId: {}]",
					message.getClass().getName(),
					message.getId());
			logger.error(errorMessage);
			finishCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorMessage);
		}
			
		return EJokerFutureUtil.completeFuture();
	}
	
	private Future<Void> handleCommandInternal(ProcessingCommand processingCommand,
			ICommandHandlerProxy commandHandler) {
		
		ICommand command = processingCommand.getMessage();
		ICommandExecuteContext commandContext = processingCommand.getCommandExecuteContext();
		
		commandContext.clear();
		
		if(processingCommand.isDuplicated()) {
			return republishCommandEvents(processingCommand);
		}
		
		ioHelper.tryAsyncAction2(
				"HandleCommandAsync", 
				() -> {

					/// TODO @await
					await(commandHandler.handleAsync(commandContext, command));
					if(logger.isDebugEnabled()) {
						logger.debug("Handle command success. [handlerType: {}, commandType: {}, commandId: {}, aggregateRootId: {}]",
	                        commandHandler.getInnerObject().getClass().getName(),
	                        command.getClass().getName(),
	                        command.getId(),
	                        command.getAggregateRootId());
					}
					return EJokerFutureUtil.completeFuture();
				},
				() -> {
					if(null != commandContext.getApplicationMessage()) {
						commitChangesAsync(processingCommand, true, commandContext.getApplicationMessage(), null);
					} else {
						try {
							commitAggregateChanges(processingCommand);
						} catch (RuntimeException ex) {
							logger.error("Commit aggregate changes has unknown exception. [handlerType: {}, commandType: {}, commandId: {}, aggregateRootId: {}]",
									commandHandler.getInnerObject().getClass().getName(),
			                        command.getClass().getName(),
			                        command.getId(),
			                        command.getAggregateRootId(),
									ex);
							finishCommandAsync(processingCommand, CommandStatus.Failed, ex.getClass().getName(), "Unknown exception caught when committing changes of command.");
						}
					}
				},
				() -> StringUtilx.fmt("[commandId: {}, commandType: {}, handlerType: {}, aggregateRootId: {}]",
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
					String errorInfo = StringUtilx.fmt(
							"Detected more than one aggregate created or modified by command!!! [commandType: {} commandId: {}]",
							command.getClass().getName(),
							command.getId()
					);
					logger.error(errorInfo);
					finishCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorInfo);
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
			republishCommandEvents(processingCommand);
			return;
		}

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
		
		await(memoryCache.acceptAggregateRootChanges(dirtyAggregateRoot));
		
        //将事件流提交到EventStore
		// TODO event提交从这里开始(可以作为调试点)
		eventCommittingService.commitDomainEventAsync(new EventCommittingContext(eventStream, processingCommand));
		
	}
	
    private Future<Void> republishCommandEvents(ProcessingCommand processingCommand) {
    	ICommand command = processingCommand.getMessage();
    	ioHelper.tryAsyncAction2(
    			"RepublishCommandEvents",
    			() -> eventStore.findAsync(command.getAggregateRootId(), command.getId()),
    			existingEventStream -> {
                    if (null != existingEventStream) {
                        eventCommittingService.publishDomainEventAsync(processingCommand, existingEventStream);
                    } else {
                    	finishCommandAsync(processingCommand, CommandStatus.NothingChanged, String.class.getName(), processingCommand.getCommandExecuteContext().getResult());
                    }
                },
    			() -> StringUtilx.fmt("[commandId: {}]", command.getId()),
    			true
    			);
		return EJokerFutureUtil.completeFuture();
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
	                	Exception realException = getRealException(exception);
	                    if (realException instanceof IDomainException) {
	                        publishExceptionAsync(processingCommand, (IDomainException )realException);
	                    } else {
	                        finishCommandAsync(processingCommand, CommandStatus.Failed, realException.getClass().getName(),
	                        		StringUtilx.isSenseful(realException.getMessage()) ? realException.getMessage() : errorMessage);
	                    }
	                    
	                } },
				() -> StringUtilx.fmt("[commandId: {}, commandType: {}, handlerType: {}, aggregateRootId: {}]",
						command.getId(),
						command.getClass().getName(),
						commandHandler.getInnerObject().getClass().getName(),
						command.getAggregateRootId()),
				true
				);
	}

    private Exception getRealException(Exception exception) {
        if (exception == null) {
            return null;
        }
        for( ;; ) {
        	// 对特定情况的异常进行扒皮操作。
        	if (exception instanceof AsyncWrapperException) {
        		exception = AsyncWrapperException.getActuallyCause(exception);
	        } else if (exception instanceof InvocationTargetException) {
	        	exception = (Exception )exception.getCause();
	        } else
	        	break;
        }
        return exception;
    }
	
	
	private void publishExceptionAsync(ProcessingCommand processingCommand, IDomainException exception) {
		exception.mergeItems(processingCommand.getMessage().getItems());
		ioHelper.tryAsyncAction2(
				"PublishExceptionAsync",
				() -> exceptionPublisher.publishAsync(exception),
				r -> finishCommandAsync(processingCommand, CommandStatus.Failed, exception.getClass().getName(), ((Exception )exception).getMessage()),
				() -> StringUtilx.fmt("[commandId: {}, exceptionType: {}, exceptionInfo: {}]",
						processingCommand.getMessage().getId(),
						exception.getClass().getName(),
						DomainExceptionCodecHelper.serialize(exception)),
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
				finishCommandAsync(processingCommand, CommandStatus.Success, null, null);
			}
		} else {
			finishCommandAsync(processingCommand, CommandStatus.Failed, String.class.getName(), errorMessage);
		}
	}

	private void publishMessageAsync(ProcessingCommand processingCommand, IApplicationMessage message) {
		ICommand command = processingCommand.getMessage();
		
		ioHelper.tryAsyncAction2(
				"PublishApplicationMessageAsync",
				() -> applicationMessagePublisher.publishAsync(message),
				r -> finishCommandAsync(processingCommand, CommandStatus.Success, message.getClass().getName(), jsonSerializer.convert(message)),
				() -> StringUtilx.fmt("[applicationMessageId: {}, applicationMessageIype: {}, commandId: {}, commandType: {}]",
						message.getId(), message.getClass().getName(), command.getId(), command.getClass().getName()),
				ex -> logger.error("Publish application message has unknown exception, the code should not be run to here!!! [errorMessage: {}]", ex.getMessage(), ex),
				true
				);
	}

	private void finishCommandAsync(ProcessingCommand processingCommand,
			CommandStatus commandStatus, String resultType, String result) {
		CommandResult commandResult = new CommandResult(commandStatus, processingCommand.getMessage().getId(),
				processingCommand.getMessage().getAggregateRootId(), result, resultType);
		
        // TODO @await
		await(processingCommand.getMailBox().finishMessage(processingCommand, commandResult));
	}
	
}