package com.jiefzz.ejoker.commanding.impl;

import java.io.IOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandStatus;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandExecuteContext;
import com.jiefzz.ejoker.commanding.ICommandHandlerPrivider;
import com.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IMemoryCache;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.EventCommittingContext;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventService;
import com.jiefzz.ejoker.eventing.IEventStore;
import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import com.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionCodecHelper;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.io.IOExceptionOnRuntime;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.io.IOHelper.IOActionExecutionContext;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.EJokerFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.helper.StringHelper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

import co.paralleluniverse.fibers.SuspendExecution;

@EService
public class DefaultProcessingCommandHandler implements IProcessingCommandHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultProcessingCommandHandler.class);

	@Dependence
	private IJSONConverter jsonSerializer;

	@Dependence
	private IEventStore eventStore;
	
	@Dependence
	private ICommandHandlerPrivider commandHandlerPrivider;
	
	@Dependence
	private IEventService eventService;

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
	private EJokerAsyncHelper eJokerAsyncHelper;
	
	@Override
	public SystemFutureWrapper<Void> handleAsync(ProcessingCommand processingCommand) {
		return systemAsyncHelper.submit(() -> handle(processingCommand));
	}

	@Override
	public void handle(ProcessingCommand processingCommand) {
		ICommand message = processingCommand.getMessage();
		if (StringHelper.isNullOrEmpty(message.getAggregateRootId())) {
			String errorInfo = String.format(
					"The aggregateId of commmandis null or empty! commandType=%s commandId=%s.", message.getTypeName(),
					message.getId());
			logger.error(errorInfo);
			completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), errorInfo);
		}

		try {
			ICommandHandlerProxy handler = commandHandlerPrivider.getHandler(message.getClass());
			handleCommand(processingCommand, handler);
//			return handleCommandAsync(processingCommand, handler);
		} catch (RuntimeException ex) {
			logger.error(ex.getMessage());
			ex.printStackTrace();
			completeCommand(processingCommand, CommandStatus.Failed, String.class.getName(), ex.getMessage());
		}

	}
	
	private void handleCommand(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler) {

		ICommand message = processingCommand.getMessage();

		processingCommand.getCommandExecuteContext().clear();

		boolean b = false;
			/// TODO @await java直接同步实现
		try {
			commandHandler.handle(processingCommand.getCommandExecuteContext(), message);
			logger.debug("Handle command success. [handlerType={}, commandType={}, commandId={}, aggregateRootId={}]",
					commandHandler.toString(), message.getTypeName(), message.getId(), message.getAggregateRootId());
			b = true;
		} catch (Exception ex) {
			handleExceptionAsync(processingCommand, commandHandler, ex);
		}

		if (b) {
			try {
				// TOTO 事件过程的起点
				commitAggregateChanges(processingCommand);
			} catch (RuntimeException ex) {
				logCommandExecuteException(processingCommand, commandHandler, ex);
				/// TODO @await
				if(EJokerEnvironment.ASYNC_ALL)
					try {
						completeCommandAsync(processingCommand, CommandStatus.Failed, ex.getClass().getName(),
								"Unknow exception caught when committing changes of command.").get();
					} catch (SuspendExecution s) {
						throw new AssertionError(s);
					}
				else 
					completeCommand(processingCommand, CommandStatus.Failed, ex.getClass().getName(),
							"Unknow exception caught when committing changes of command.");
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
			if(null!=changes && changes.size()>0) {
				dirtyAggregateRootCount++;
				if(dirtyAggregateRootCount>1) {
					String errorInfo = String.format(
							"Detected more than one aggregate created or modified by command!!! commandType=%s commandId=%s",
							command.getTypeName(),
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
		if(0 == dirtyAggregateRootCount || null == changeEvents || 0 == changeEvents.size() ) {
			processIfNoEventsOfCommand(processingCommand);
			return;
		}

        //构造出一个事件流对象
		DomainEventStream eventStream = buildDomainEventStream(dirtyAggregateRoot, changeEvents, processingCommand);

        //将事件流提交到EventStore
		// TODO event提交从这里开始(可以作为调试点)
		eventService.commitDomainEventAsync(new EventCommittingContext(dirtyAggregateRoot, eventStream, processingCommand));
		
	}
	
	private DomainEventStream buildDomainEventStream(IAggregateRoot aggregateRoot, Collection<IDomainEvent<?>> changeEvents, ProcessingCommand processingCommand) {
		String result = processingCommand.getCommandExecuteContext().getResult();
		if(null!=result)
			processingCommand.getItems().put("CommandResult", result);
		
		return new DomainEventStream(
				processingCommand.getMessage().getId(),
				aggregateRoot.getUniqueId(),
				aggregateRoot.getClass().getName(),
				aggregateRoot.getVersion()+1,
				System.currentTimeMillis(),
				changeEvents,
				processingCommand.getItems()
		);
		
	}
	
    private void processIfNoEventsOfCommand(ProcessingCommand processingCommand) {
    	ICommand command = processingCommand.getMessage();
    	ioHelper.tryAsyncAction(new IOActionExecutionContext<DomainEventStream>(true) {

			@Override
			public String getAsyncActionName() {
				return "ProcessIfNoEventsOfCommand";
			}

			@Override
			public SystemFutureWrapper<AsyncTaskResult<DomainEventStream>> asyncAction() throws SuspendExecution {
				return eventStore.findAsync(command.getAggregateRootId(), command.getId());
			}

			@Override
			public void finishAction(DomainEventStream result) throws SuspendExecution {
				DomainEventStream existingEventStream = result;
                if (null != existingEventStream) {
                    eventService.publishDomainEventAsync(processingCommand, existingEventStream);
                } else {
                    completeCommandAsync(processingCommand, CommandStatus.NothingChanged, String.class.getName(), processingCommand.getCommandExecuteContext().getResult());
                }
			}

			@Override
			public void faildAction(Exception ex) throws SuspendExecution {
				logger.error(
						String.format("Find event by commandId has unknown exception, the code should not be run to here, errorMessage: {}",
								ex.getMessage()),
						ex);
			}

			@Override
			public String getContextInfo() {
				return String.format("[commandId: %s]", command.getId());
			}
			
    	});
    }
	
	private void handleExceptionAsync(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler, Exception exception) {
		ICommand command = processingCommand.getMessage();
		
		ioHelper.tryAsyncAction(new IOActionExecutionContext<DomainEventStream>(true) {

			@Override
			public String getAsyncActionName() {
				return "FindEventByCommandIdAsync";
			}

			@Override
			public SystemFutureWrapper<AsyncTaskResult<DomainEventStream>> asyncAction() throws SuspendExecution {
				return eventStore.findAsync(command.getAggregateRootId(), command.getId());
			}

			@Override
			public void finishAction(DomainEventStream result) throws SuspendExecution {
				
				DomainEventStream existingEventStream = result;
                if (existingEventStream != null) {
                    //这里，我们需要再重新做一遍发布事件这个操作；
                    //之所以要这样做是因为虽然该command产生的事件已经持久化成功，但并不表示事件已经发布出去了；
                    //因为有可能事件持久化成功了，但那时正好机器断电了，则发布事件就没有做；
                    eventService.publishDomainEventAsync(processingCommand, existingEventStream);
                
                } else {
                	
                    //到这里，说明当前command执行遇到异常，然后当前command之前也没执行过，是第一次被执行。
                    //那就判断当前异常是否是需要被发布出去的异常，如果是，则发布该异常给所有消费者；
                    //否则，就记录错误日志，然后认为该command处理失败即可；
                	IPublishableException publishableException
                		= (exception instanceof IPublishableException) ? (IPublishableException )exception : null;
                    if (publishableException != null) {
                        publishExceptionAsync(processingCommand, publishableException);
                    } else {
                        logCommandExecuteException(processingCommand, commandHandler, exception);
                        completeCommandAsync(processingCommand, CommandStatus.Failed, exception.getClass().getName(), exception.getMessage());
                    }
                    
                }
			}

			@Override
			public void faildAction(Exception ex) throws SuspendExecution {
				logger.error(
						String.format("Find event by commandId has unknown exception, the code should not be run to here, errorMessage: {}",
								ex.getMessage()),
						ex);
			}

			@Override
			public String getContextInfo() {
				return String.format("[commandId: %s]", command.getId());
			}
			
		});
		
	}
	
	
	private void publishExceptionAsync(ProcessingCommand processingCommand, IPublishableException exception) {
		ioHelper.tryAsyncAction(new IOActionExecutionContext<Void>(true) {

			@Override
			public String getAsyncActionName() {
				return "PublishExceptionAsync";
			}

			@Override
			public SystemFutureWrapper<AsyncTaskResult<Void>> asyncAction() throws SuspendExecution {
				return exceptionPublisher.publishAsync(exception);
			}

			@Override
			public void finishAction(Void result) throws SuspendExecution {
				completeCommandAsync(processingCommand, CommandStatus.Failed, exception.getClass().getName(), ((Exception )exception).getMessage());
			}

			@Override
			public void faildAction(Exception ex) throws SuspendExecution {
				logger.error(String.format("Publish event has unknown exception, the code should not be run to here, errorMessage: {}", ex.getMessage()), ex);
			}

			@Override
			public String getContextInfo() {
                return String.format("[commandId: %s, exceptionType: %s, exceptionInfo: %s]", processingCommand.getMessage().getId(), exception.getClass().getName(), PublishableExceptionCodecHelper.serialize(exception));
            }
			
		});
		
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
	
	private SystemFutureWrapper<AsyncTaskResult<Void>> handleCommandAsync(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler) {
		ICommand command = processingCommand.getMessage();
		
		return eJokerAsyncHelper.submit(() -> {
			
			ioHelper.tryAsyncAction(new IOActionExecutionContext<Void>() {

				@Override
				public String getAsyncActionName() {
					return "HandleCommandAsync";
				}

				@Override
				public SystemFutureWrapper<AsyncTaskResult<Void>> asyncAction() throws SuspendExecution {
					try {
						commandHandler.handle(processingCommand.getCommandExecuteContext(), command);
						logger.debug("Handle command async success. handler:{}, commandType:{}, commandId:{}, aggregateRootId:{}",
	                            commandHandler.toString(),
	                            command.getClass().getName(),
	                            command.getId(),
	                            command.getAggregateRootId());
						
						return EJokerFutureWrapperUtil.createCompleteFutureTask();
					} catch (Exception ex) {
						
						while(ex instanceof IOExceptionOnRuntime)
							ex = (Exception )ex.getCause();
						
	                    logger.error(String.format("Handle command async has io exception. handler:%s, commandType:%s, commandId:%s, aggregateRootId:%s",
	                    		commandHandler.toString(),
	                            command.getClass().getName(),
	                            command.getId(),
	                            command.getAggregateRootId()), ex);

	            		RipenFuture<AsyncTaskResult<Void>> rf = new RipenFuture<>();
	            		rf.trySetResult(new AsyncTaskResult<>(ex instanceof IOException ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed, ex.getMessage()));
	                    return new SystemFutureWrapper<>(rf);
	                }
				}

				@Override
				public void finishAction(Void result) throws SuspendExecution {
					commitChangesAsync(processingCommand, true, null, null);
				}

				@Override
				public void faildAction(Exception ex) throws SuspendExecution {
					commitChangesAsync(processingCommand, false, null, ex.getMessage());
				}

				@Override
				public String getContextInfo() {
					return String.format("[command: [id: %s, type: %s], handler: %s]", command.getId(), command.getClass().getName(), commandHandler.toString());
				}
				
			});
			
		});
    }
	
	private void commitChangesAsync(ProcessingCommand processingCommand, boolean success, IApplicationMessage message,
			String errorMessage) {
		if (success) {
			if (null != message) {
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
		
		ioHelper.tryAsyncAction(new IOActionExecutionContext<Void>(true) {

			@Override
			public String getAsyncActionName() {
				return "PublishApplicationMessageAsync";
			}

			@Override
			public SystemFutureWrapper<AsyncTaskResult<Void>> asyncAction() throws SuspendExecution {
				return applicationMessagePublisher.publishAsync(message);
			}

			@Override
			public void finishAction(Void result) throws SuspendExecution {
				completeCommandAsync(processingCommand, CommandStatus.Success, message.getClass().getName(), jsonSerializer.convert(message));
			}

			@Override
			public void faildAction(Exception ex) throws SuspendExecution {
				logger.error(String.format("Publish application message has unknown exception, the code should not be run to here, errorMessage: {}", ex.getMessage()), ex);
			}

			@Override
			public String getContextInfo() {
				return String.format("[application message:[id: %, type: %s],command:[id: %s, type: %s]]", message.getId(), message.getClass().getName(), command.getId(), command.getClass().getName());
			}
			
		});
	}

	private SystemFutureWrapper<AsyncTaskResult<Void>> completeCommandAsync(ProcessingCommand processingCommand,
			CommandStatus commandStatus, String resultType, String result) {
		CommandResult commandResult = new CommandResult(commandStatus, processingCommand.getMessage().getId(),
				processingCommand.getMessage().getAggregateRootId(), result, resultType);
		// TODO 完成传递
		return processingCommand.getMailbox().completeMessageAsync(processingCommand, commandResult);
	}

	private void completeCommand(ProcessingCommand processingCommand,
			CommandStatus commandStatus, String resultType, String result) {
		CommandResult commandResult = new CommandResult(commandStatus, processingCommand.getMessage().getId(),
				processingCommand.getMessage().getAggregateRootId(), result, resultType);
		processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
	}
	
}