package com.jiefzz.ejoker.commanding.impl;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.ICommandProcessor;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.IProcessingCommandScheduler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import com.jiefzz.ejoker.context.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.z.common.ArgumentException;

@EService
public class DefaultCommandProcessorImpl implements ICommandProcessor {

	private final static  Logger logger = LoggerFactory.getLogger(DefaultCommandProcessorImpl.class);
	
	// TODO: C# use ConcurrentDictionary here.
	private final ConcurrentHashMap<String, ProcessingCommandMailbox> mailboxDict = 
			new ConcurrentHashMap<String, ProcessingCommandMailbox>();

	@Resource
    private IProcessingCommandScheduler scheduler;
	@Resource
    private IProcessingCommandHandler handler;
	
	@Override
	public void process(ProcessingCommand processingCommand) {
		String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId==null || "".equals(aggregateRootId))
            throw new ArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().getId());

        // TODO: 调试检查起正确性！！！！
        // TODO: 调试检查起正确性！！！！
        // TODO: 调试检查起正确性！！！！
        ProcessingCommandMailbox mailbox = mailboxDict.putIfAbsent(aggregateRootId, new ProcessingCommandMailbox(aggregateRootId, scheduler, handler));
        mailbox.enqueueMessage(processingCommand);
	}

}
