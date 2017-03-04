package com.jiefzz.ejoker.infrastructure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.z.common.system.helper.StringHelper;

public abstract class AbstractMessageProcessor<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IMessageProcessor<X, Y> {

	private final Lock lock4tryCreateMailbox = new ReentrantLock();

	private final Map<String, ProcessingMessageMailbox<X, Y>> mailboxDict = new ConcurrentHashMap<String, ProcessingMessageMailbox<X, Y>>();

	protected abstract IProcessingMessageScheduler<X, Y> getProcessingMessageScheduler();
	protected abstract IProcessingMessageHandler<X, Y> getProcessingMessageHandler();

	private int timeout = 30;

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
						mailboxDict.put(routingKey, new ProcessingMessageMailbox<X, Y>(routingKey,
								getProcessingMessageScheduler(), getProcessingMessageHandler()));
					}
					process(processingMessage);
				} finally {
					lock4tryCreateMailbox.unlock();
				}
			} else
				mailbox.enqueueMessage(processingMessage);
		} else {
			getProcessingMessageScheduler().scheduleMessage(processingMessage);
		}
	}
}
