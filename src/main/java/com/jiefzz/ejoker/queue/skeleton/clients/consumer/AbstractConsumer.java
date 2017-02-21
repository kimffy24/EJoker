package com.jiefzz.ejoker.queue.skeleton.clients.consumer;

import com.jiefzz.ejoker.z.common.utilities.Ensure;

public abstract class AbstractConsumer implements IConsumer {

	protected IMessageHandler commandConsumer=null;
	
	@Override
	public IConsumer setMessageHandler(IMessageHandler commandConsumer) {
		Ensure.notNull(commandConsumer, "commandConsumer");
		this.commandConsumer = commandConsumer;
		return this;
	}
	
}
