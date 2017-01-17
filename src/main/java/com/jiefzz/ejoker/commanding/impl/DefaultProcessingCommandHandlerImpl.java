package com.jiefzz.ejoker.commanding.impl;

import java.util.Collection;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandStatus;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandExecuteContext;
import com.jiefzz.ejoker.commanding.ICommandHandlerPrivider;
import com.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.eventing.DomainEventStream;
import com.jiefzz.ejoker.eventing.EventCommittingConetxt;
import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventService;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.helper.StringHelper;

@EService
public class DefaultProcessingCommandHandlerImpl implements IProcessingCommandHandler {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultProcessingCommandHandlerImpl.class);

	@Resource
	IJSONConverter jsonSerializer;
	@Resource
	ICommandHandlerPrivider commandHandlerPrivider;
	@Resource
	IEventService eventService;
	
	@Override
	public void handle(ProcessingCommand processingCommand) {
		
		ICommand message = processingCommand.getMessage();
		if(StringHelper.isNullOrEmpty(message.getAggregateRootId())) {
			String errorInfo = String.format("The aggregateId of commmandis null or empty! commandType=[%s] commandId=%s.", message.getTypeName(), message.getId());
			logger.error(errorInfo);
			completeMessage(processingCommand, CommandStatus.Failed, String.class.getName(), errorInfo);
		}
		
		try {
			ICommandHandlerProxy handler = commandHandlerPrivider.getHandler(message.getClass());
			handleCommand(processingCommand, handler);
		} catch( Exception e ) {
			logger.error(e.getMessage());
			e.printStackTrace();
			completeMessage(processingCommand, CommandStatus.Failed, String.class.getName(), e.getMessage());
		}
	}
	
	private void handleCommand(ProcessingCommand processingCommand, ICommandHandlerProxy commandHandler) {
		ICommand message = processingCommand.getMessage();
		processingCommand.getCommandExecuteContext().clear();
		
		boolean handleSuccess = false;
		try {
			commandHandler.hadler(processingCommand.getCommandExecuteContext(), message);
			logger.debug("Handle command success. [handlerType={}, commandType={}, commandId={}, aggregateRootId={}]", commandHandler.toString(), message.getTypeName(), message.getId(), message.getAggregateRootId());
			handleSuccess = true;
		} catch( Exception e ) {
			// TODO 此处应该进入EJoker的异常发布过程
			// TODO 此处应该进入EJoker的异常发布过程
			// TODO 此处应该进入EJoker的异常发布过程
			return;
		}
		
		if(handleSuccess) {
			try {
				// TOTO 事件过程的起点
				commitAggregateChanges(processingCommand);
			} catch( Exception e ) {
				logger.error("{} raise when {} handling {}. commandId={}, aggregateId={}", e.getMessage(), commandHandler.toString(), message.getId(), message.getAggregateRootId());
				completeMessage(processingCommand, CommandStatus.Failed, e.getClass().getName(), "Unknow exception caught when committing changes of command.");
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
					completeMessage(processingCommand, CommandStatus.Failed, String.class.getName(), errorInfo);
				}
				dirtyAggregateRoot=aggregateRoot;
				changeEvents = changes;
			}
		}
		
		// if nothing change
		if(dirtyAggregateRootCount==0 || changeEvents==null || changeEvents.size()==0) {
			completeMessage(processingCommand, CommandStatus.Failed, String.class.getName(), context.getResult());
			return;
		}
		
		DomainEventStream eventStream = buildDomainEventStream(dirtyAggregateRoot, changeEvents, processingCommand);
		
		// TODO event发布从这里开始(可以作为调试点)
		eventService.commitDomainEventAsync(new EventCommittingConetxt(dirtyAggregateRoot, eventStream, processingCommand));
		
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
	
	private void completeMessage(ProcessingCommand processingCommand, CommandStatus commandStatus, String resultType, String result) {
		CommandResult commandResult = new CommandResult(commandStatus, processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId(), result, resultType);
		processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
		processingCommand.getMailbox().tryExecuteNextMessage();
	}
	
}