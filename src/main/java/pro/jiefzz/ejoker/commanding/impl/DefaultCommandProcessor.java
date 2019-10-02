package pro.jiefzz.ejoker.commanding.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.commanding.ICommandProcessor;
import pro.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import pro.jiefzz.ejoker.commanding.ProcessingCommand;
import pro.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.exceptions.ArgumentException;
import pro.jiefzz.ejoker.z.service.IScheduleService;
import pro.jiefzz.ejoker.z.service.Scavenger;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;

/**
 * 默认的命令处理类<br>
 * @author jiefzz
 *
 */
@EService
public final class DefaultCommandProcessor implements ICommandProcessor {

	private final static Logger logger = LoggerFactory.getLogger(DefaultCommandProcessor.class);
	
	private final Map<String, ProcessingCommandMailbox> mailboxDict = new ConcurrentHashMap<>();

	@Dependence
    private IProcessingCommandHandler handler;

	@Dependence
	private IScheduleService scheduleService;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	@Dependence
	private Scavenger scavenger;
	
	private long timeoutMillis = EJokerEnvironment.MAILBOX_IDLE_TIMEOUT;
	
	private long cleanInactivalMillis = EJokerEnvironment.IDLE_RELEASE_PERIOD;
	
	@EInitialize
	private void init() {

		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				this::cleanInactiveMailbox,
				cleanInactivalMillis,
				cleanInactivalMillis);
		
	}

	@Override
	public void process(ProcessingCommand processingCommand) {
		String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId==null || "".equals(aggregateRootId))
            throw new ArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().getId());

//        // 虽然内存操作比IO快得多得多，但是这样全部command开始都在这里竞争真的好吗？
//        asyncLock.lock();
//        try {
//            ProcessingCommandMailbox mailbox = MapHelper.getOrAddConcurrent(mailboxDict, aggregateRootId, () -> new ProcessingCommandMailbox(aggregateRootId, handler, systemAsyncHelper));
//            mailbox.enqueueMessage(processingCommand);
//        } finally {
//        	asyncLock.unlock();
//        }

        ProcessingCommandMailbox mailbox;
        
        do {
			mailbox = MapHelper.getOrAddConcurrent(mailboxDict, aggregateRootId, () -> new ProcessingCommandMailbox(aggregateRootId, handler, systemAsyncHelper));
        	if(mailbox.tryUse()) {
        		// tryUse()包装的是读锁，当前这个process调用是可以并行的。
        		try {
        			mailbox.enqueueMessage(processingCommand);
					break;
        		} finally {
        			mailbox.releaseUse();
        		}
        	} else {
        		// ... 不排队队，纯自旋 ...
        	}
        } while (true);
	}

	/**
	 * clean long time idle mailbox
	 * 清理超时mailbox的函数。<br>
	 */
	private void cleanInactiveMailbox() {
		Iterator<Entry<String, ProcessingCommandMailbox>> it = mailboxDict.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, ProcessingCommandMailbox> current = it.next();
			ProcessingCommandMailbox mailbox = current.getValue();
			if(mailbox.isInactive(timeoutMillis)
					&& mailbox.tryClean()
					) {
		        // tryClean()封装的是一个写锁，即clean过程完全排他的
				// 获取失败后马上放弃clean过程，可能有别的线程要使用mailbox
		        try {
		        	if(mailbox.getTotalUnConsumedMessageCount() > 0) {
		        		continue;
		        	}
		        	it.remove();
		        	logger.debug("Removed inactive command mailbox, aggregateRootId: {}", current.getKey());
		        } finally {
		        	mailbox.releaseClean();
		        }
			}
		}
	}
}
