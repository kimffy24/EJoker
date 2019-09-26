package pro.jiefzz.ejoker.commanding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.infrastructure.AbstractMailBox;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapperUtil;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;

public class ProcessingCommandMailbox extends AbstractMailBox<ProcessingCommand, CommandResult> {

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
			logger.error(String.format("Failed to complete command, commandId: %s, aggregateRootId: %s, commandType: %s", processingCommand.getMessage().getId(), getRoutingKey(), processingCommand.getMessage().getClass().getSimpleName()), ex);
			return SystemFutureWrapperUtil.completeFuture();
		}
	}
	
}