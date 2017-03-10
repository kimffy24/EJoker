package com.jiefzz.ejoker.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.system.helper.StringHelper;

public abstract class AbstractMessageProcessor<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IMessageProcessor<X, Y> {

	private final static Logger logger = LoggerFactory.getLogger(AbstractMessageProcessor.class);
	
	private final Lock lock4tryCreateMailbox = new ReentrantLock();

	private final Map<String, ProcessingMessageMailbox<X, Y>> mailboxDict = new ConcurrentHashMap<String, ProcessingMessageMailbox<X, Y>>();

	@Dependence
	IScheduleService scheduleService;
	
	protected abstract IProcessingMessageScheduler<X, Y> getProcessingMessageScheduler();
	protected abstract IProcessingMessageHandler<X, Y> getProcessingMessageHandler();

	public abstract String getMessageName();

	public void process(X processingMessage) {
		String routingKey = processingMessage.getMessage().getRoutingKey();
		if (!StringHelper.isNullOrWhiteSpace(routingKey)) {
			ProcessingMessageMailbox<X, Y> mailbox;
			if (null == (mailbox = mailboxDict.getOrDefault(routingKey, null))) {
				// ah, here use a dangerous minds.
				lock4tryCreateMailbox.lock();
				try {
					if (!mailboxDict.containsKey(routingKey)) {
						mailboxDict.put(routingKey, (mailbox = new ProcessingMessageMailbox<X, Y>(routingKey,
								getProcessingMessageScheduler(), getProcessingMessageHandler())));
					}
					// process(processingMessage);
					mailbox.enqueueMessage(processingMessage);
				} finally {
					lock4tryCreateMailbox.unlock();
				}
			} else
				mailbox.enqueueMessage(processingMessage);
		} else {
			getProcessingMessageScheduler().scheduleMessage(processingMessage);
		}
	}
	
	// clean long time idle mailbox
	
	/**
	 * 清理超时mailbox的函数。<br>
	 */
	private void cleanInactiveMailbox() {
		List<String> idelMailboxKeyList = new ArrayList<String>();
		Set<Entry<String,ProcessingMessageMailbox<X,Y>>> entrySet = mailboxDict.entrySet();
		for(Entry<String,ProcessingMessageMailbox<X,Y>> entry:entrySet) {
			ProcessingMessageMailbox<X,Y> processingMailbox = entry.getValue();
			if(!processingMailbox.isRunning() && processingMailbox.isInactive(EJokerEnvironment.MAILBOX_IDLE_TIMEOUT))
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
			logger.info("Removed inactive {} mailbox, aggregateRootId: {}", getMessageName(), mailboxKey);
		}
	}
	
	@EInitialize
	public void init() {
		scheduleService.StartTask(this.getClass().getName() +"#cleanInactiveMailbox()", new Runnable() {
			@Override
			public void run() {
				AbstractMessageProcessor.this.cleanInactiveMailbox();
			}
		}, EJokerEnvironment.MAILBOX_IDLE_TIMEOUT, EJokerEnvironment.MAILBOX_IDLE_TIMEOUT);
	}
	
}
