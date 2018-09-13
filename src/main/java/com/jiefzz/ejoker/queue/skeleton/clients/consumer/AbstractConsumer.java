package com.jiefzz.ejoker.queue.skeleton.clients.consumer;

import com.jiefzz.ejoker.queue.skeleton.IQueueComsumerWokerService;
import com.jiefzz.ejoker.z.common.utils.Ensure;

public abstract class AbstractConsumer implements IConsumer {

	protected IEJokerQueueMessageHandler commandConsumer=null;
	
	@Override
	public IConsumer setMessageHandler(IEJokerQueueMessageHandler commandConsumer) {
		Ensure.notNull(commandConsumer, "commandConsumer");
		this.commandConsumer = commandConsumer;
		return this;
	}
	
	public IConsumer getConsumer() { return this; }
	public IQueueComsumerWokerService useConsumer(IConsumer consumer) { return this; }
	
}
