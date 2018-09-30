package com.jiefzz.ejoker.commanding.impl;

import java.util.concurrent.ConcurrentHashMap;

import com.jiefzz.ejoker.commanding.ICommandProcessor;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import com.jiefzz.ejoker.z.common.ArgumentException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;

/**
 * 默认的命令处理类<br>
 * @author jiefzz
 *
 */
@EService
public final class DefaultCommandProcessorImpl implements ICommandProcessor {

	private final ConcurrentHashMap<String, ProcessingCommandMailbox> mailboxDict = new ConcurrentHashMap<>();
	
	@Dependence
    private IProcessingCommandHandler handler;
	
	@Override
	public void process(ProcessingCommand processingCommand) {
		
		String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId==null || "".equals(aggregateRootId))
            throw new ArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().getId());

        ProcessingCommandMailbox mailbox = MapHelper.getOrAddConcurrent(mailboxDict, aggregateRootId, () -> new ProcessingCommandMailbox(aggregateRootId, handler));
        mailbox.enqueueMessage(processingCommand);
	}

}
