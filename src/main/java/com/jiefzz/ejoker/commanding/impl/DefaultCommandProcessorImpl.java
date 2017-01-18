package com.jiefzz.ejoker.commanding.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.ICommandProcessor;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.IProcessingCommandScheduler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import com.jiefzz.ejoker.z.common.ArgumentException;
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

	@Resource
    private IProcessingCommandScheduler scheduler;
	@Resource
    private IProcessingCommandHandler handler;
	
	@Override
	public void process(ProcessingCommand processingCommand) {
		String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId==null || "".equals(aggregateRootId))
            throw new ArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().getId());

        ProcessingCommandMailbox mailbox;
        /**
         * C# GetOrAdd 方法能做到不存在则构建。 Java没有。
         * TODO: 调试检查起正确性！！！！ 是不是可以考虑下使用 putIfAbsent 呢
         */
        if(null==(mailbox = mailboxDict.getOrDefault(aggregateRootId, null))) {
        	lock4tryCreateMailbox.lock();
        	try {
        		if(!mailboxDict.containsKey(aggregateRootId)){	// 若发生竟态，获取锁后，先检查在此线程之前获取锁的线程是否创建了Mailbox
        			// 在递归调用时似乎会发生死锁？ 似乎在递归调用时不会再进入这个if语句块才是合乎逻辑的
        			logger.debug("Creating mailbox for aggregateRoot[aggregateRootId={}].", aggregateRootId);
                	mailboxDict.put(aggregateRootId, new ProcessingCommandMailbox(aggregateRootId, scheduler, handler));
        		}
    			process(processingCommand);
        	} finally {
        		lock4tryCreateMailbox.unlock();
        	}
        } else
        	mailbox.enqueueMessage(processingCommand);
	}

}
