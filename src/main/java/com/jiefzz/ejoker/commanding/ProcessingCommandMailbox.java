package com.jiefzz.ejoker.commanding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.infrastructure.AbstractAggregateMessageMailBox;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

public class ProcessingCommandMailbox extends AbstractAggregateMessageMailBox<ProcessingCommand, CommandResult> {

	private final static Logger logger = LoggerFactory.getLogger(ProcessingCommandMailbox.class);

	public ProcessingCommandMailbox(String aggregateRootId, IProcessingCommandHandler messageHandler,
			SystemAsyncHelper systemAsyncHelper) {
		super(aggregateRootId, EJokerEnvironment.MAX_BATCH_COMMANDS, false, x -> messageHandler.handle(x), null, systemAsyncHelper);
	}

	@Override
	protected SystemFutureWrapper<Void> completeMessageWithResult(ProcessingCommand processingCommand, CommandResult commandResult) {
		try {
			return processingCommand.completeAsync(commandResult);
		} catch (RuntimeException ex) {
			logger.error(String.format("Failed to complete command, commandId: %s, aggregateRootId: %s, commandType: %s", processingCommand.getMessage().getId(), getAggregateRootId(), processingCommand.getMessage().getClass().getSimpleName()), ex);
			return SystemFutureWrapperUtil.completeFuture();
		}
	}
	
	
}