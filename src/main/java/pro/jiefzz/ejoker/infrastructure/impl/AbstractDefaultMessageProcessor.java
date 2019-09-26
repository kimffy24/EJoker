package pro.jiefzz.ejoker.infrastructure.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJokerEnvironment;
import pro.jiefzz.ejoker.infrastructure.IMessage;
import pro.jiefzz.ejoker.infrastructure.IMessageProcessor;
import pro.jiefzz.ejoker.infrastructure.IProcessingMessage;
import pro.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import pro.jiefzz.ejoker.infrastructure.IProcessingMessageScheduler;
import pro.jiefzz.ejoker.infrastructure.ProcessingMessageMailbox;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.z.schedule.IScheduleService;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;

public abstract class AbstractDefaultMessageProcessor<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IMessageProcessor<X, Y> {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final Map<String, ProcessingMessageMailbox<X, Y>> mailboxDict = new ConcurrentHashMap<>();

	@Dependence
	private IScheduleService scheduleService;

	@Dependence
	private IProcessingMessageScheduler<X, Y> processingMessageScheduler;
	
	@Dependence
	private IProcessingMessageHandler<X, Y> processingMessageHandler;

	public abstract String getMessageName();
	
	@EInitialize
	private void init() {
		scheduleService.startTask(
				String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				this::cleanInactiveMailbox,
				2000l,
				2000l);
	}
	
	public void process(X processingMessage) {
		String routingKey = processingMessage.getMessage().getRoutingKey();
		if (!StringHelper.isNullOrWhiteSpace(routingKey)) {
			ProcessingMessageMailbox<X, Y> mailbox = MapHelper.getOrAddConcurrent(mailboxDict, routingKey, () -> new ProcessingMessageMailbox<X, Y>(routingKey,
					processingMessageScheduler, processingMessageHandler));
			mailbox.enqueueMessage(processingMessage);
		} else {
			processingMessageScheduler.scheduleMessage(processingMessage);
		}
	}
	
	/**
	 * clean long time idle mailbox
	 * 清理超时mailbox的函数。<br>
	 */
	private void cleanInactiveMailbox() {
		Iterator<Entry<String, ProcessingMessageMailbox<X, Y>>> it = mailboxDict.entrySet().iterator();
		while(it.hasNext()) {
			Entry<String, ProcessingMessageMailbox<X, Y>> current = it.next();
			ProcessingMessageMailbox<X, Y> mailbox = current.getValue();
			if(mailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT)) {
				it.remove();
				logger.debug("Removed inactive {} mailbox, aggregateRootId: {}", getMessageName(), current.getKey());
			}
		}
	}
	
}
