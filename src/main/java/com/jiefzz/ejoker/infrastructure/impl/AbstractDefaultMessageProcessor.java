package com.jiefzz.ejoker.infrastructure.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageProcessor;
import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
import com.jiefzz.ejoker.infrastructure.IProcessingMessageScheduler;
import com.jiefzz.ejoker.infrastructure.ProcessingMessageMailbox;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.system.helper.StringHelper;

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
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT);
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
