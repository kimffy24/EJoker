package com.jiefzz.ejoker.commanding.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.commanding.ICommandProcessor;
import com.jiefzz.ejoker.commanding.IProcessingCommandHandler;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.commanding.ProcessingCommandMailbox;
import com.jiefzz.ejoker.z.common.ArgumentException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;

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
	private EJokerTaskAsyncHelper eJokerAsyncHelper;

	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				this::cleanInactiveMailbox,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT);
	}

	public void d1() {
		Set<Entry<String, ProcessingCommandMailbox>> entrySet = mailboxDict.entrySet();
		for (Entry<String, ProcessingCommandMailbox> ety : entrySet) {
			ety.getValue().showLog();
		}
	}

	@Override
	public void process(ProcessingCommand processingCommand) {
		
		String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId==null || "".equals(aggregateRootId))
            throw new ArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().getId());

        ProcessingCommandMailbox mailbox = MapHelper.getOrAddConcurrent(mailboxDict, aggregateRootId, () -> new ProcessingCommandMailbox(aggregateRootId, handler, eJokerAsyncHelper));
        mailbox.enqueueMessage(processingCommand);
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
			if(!mailbox.onRunning() && mailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT)) {
				it.remove();
				logger.debug("Removed inactive command mailbox, aggregateRootId: {}", current.getKey());
			}
		}
	}
}
