package com.jiefzz.ejoker.infrastructure.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

public abstract class DefaultMessageProcessorAbstract<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IMessageProcessor<X, Y> {

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
				String.format("{}@{}#{}", this.getClass().getName(), this.hashCode(), "cleanInactiveMailbox()"),
				() -> cleanInactiveMailbox(),
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT,
				EJokerEnvironment.MAILBOX_IDLE_TIMEOUT);
	}
	
	// react调度
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
		List<String> idelMailboxKeyList = new ArrayList<>();
		Set<Entry<String,ProcessingMessageMailbox<X,Y>>> entrySet = mailboxDict.entrySet();
		for(Entry<String,ProcessingMessageMailbox<X,Y>> entry:entrySet) {
			ProcessingMessageMailbox<X,Y> processingMailbox = entry.getValue();
			if(!processingMailbox.onRunning() && processingMailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT))
				idelMailboxKeyList.add(entry.getKey());
		}
		
		for(String mailboxKey:idelMailboxKeyList) {
			ProcessingMessageMailbox<X, Y> processingMessageMailbox = mailboxDict.get(mailboxKey);
			if(!processingMessageMailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT)) {
				// 在上面判断其达到空闲条件后，又被重新使能（临界情况）
				// 放弃本次操作
				continue;
			}
			mailboxDict.remove(mailboxKey);
			logger.debug("Removed inactive {} mailbox, aggregateRootId: {}", getMessageName(), mailboxKey);
		}
	}
	
}
