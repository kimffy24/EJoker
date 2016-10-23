package com.jiefzz.ejoker.commanding.impl;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandStatus;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultProcessingCommandHandlerImpl implements IProcessingCommandHandler {
	
	private final static  Logger logger = LoggerFactory.getLogger(DefaultProcessingCommandHandlerImpl.class);

	@Resource
	IJSONConverter jsonSerializer;
	
	@Override
	public void handleAsync(ProcessingCommand processingCommand) {
		logger.debug("Receve Command: {}", jsonSerializer.convert(processingCommand.getMessage()));
		logger.debug("Receve Command sequence: {}", processingCommand.getSequence());

		logger.debug("Try complete message.");
		completeMessage(processingCommand, CommandStatus.Success, String.class.getName(), "This complete action is for test.");
	}

	public void completeMessage(ProcessingCommand processingCommand, CommandStatus commandStatus, String resultType, String result) {
		CommandResult commandResult = new CommandResult(commandStatus, processingCommand.getMessage().getId(), processingCommand.getMessage().getAggregateRootId(), result, resultType);
		processingCommand.getMailbox().completeMessage(processingCommand, commandResult);
		processingCommand.getMailbox().tryExecuteNextMessage();
	}
	
}