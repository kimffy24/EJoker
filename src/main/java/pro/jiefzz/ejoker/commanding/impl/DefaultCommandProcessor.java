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
import pro.jiefzz.ejoker.z.schedule.IScheduleService;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;

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

	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				this::cleanInactiveMailbox,
				2000l,
				2000l);
	}

	@Override
	public void process(ProcessingCommand processingCommand) {
		String aggregateRootId = processingCommand.getMessage().getAggregateRootId();
        if (aggregateRootId==null || "".equals(aggregateRootId))
            throw new ArgumentException("aggregateRootId of command cannot be null or empty, commandId:" + processingCommand.getMessage().getId());

        ProcessingCommandMailbox mailbox = MapHelper.getOrAddConcurrent(mailboxDict, aggregateRootId, () -> new ProcessingCommandMailbox(aggregateRootId, handler, systemAsyncHelper));
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
			if(!mailbox.isRunning() && mailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT)) {
				it.remove();
				logger.debug("Removed inactive command mailbox, aggregateRootId: {}", current.getKey());
			}
		}
	}
}
