package com.jiefzz.ejoker.commanding.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.ICommandProcessor;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import com.jiefzz.ejoker.z.common.ArgumentException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

/**
 * 默认的命令处理类<br>
 * 部分对锁的调用， 应禁止此类被继承改写。
 * @author jiefzz
 *
 */
@EService
public final class DefaultCommandProcessorImpl implements ICommandProcessor {

	private final static  Logger logger = LoggerFactory.getLogger(DefaultCommandProcessorImpl.class);
	
	private final Lock lock4tryCreateMailbox = new ReentrantLock();
	
	// TODO: C# use ConcurrentDictionary here.
	private final ConcurrentHashMap<String, ProcessingCommandMailbox> mailboxDict = 
			new ConcurrentHashMap<String, ProcessingCommandMailbox>();
	
	@Dependence
    private IProcessingCommandHandler handler;
	
	@Override
	public void process(ProcessingCommand processingCommand) {
		String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId==null || "".equals(aggregateRootId))
            throw new ArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().getId());

        ProcessingCommandMailbox mailbox, prevousMailbox;
        
        if(null==(mailbox = mailboxDict.getOrDefault(aggregateRootId, null))) {
        	lock4tryCreateMailbox.lock();
        	try {
        		if(null == (prevousMailbox = mailboxDict.putIfAbsent(aggregateRootId, mailbox = new ProcessingCommandMailbox(aggregateRootId, handler)))){
        			logger.debug("Creating mailbox for aggregateRoot[aggregateRootId={}].", aggregateRootId);
        		} else
        			mailbox = prevousMailbox;
        	} finally {
        		lock4tryCreateMailbox.unlock();
        	}
        }
        mailbox.enqueueMessage(processingCommand);
	}

}
