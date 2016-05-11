package com.jiefzz.ejoker.z.queue.clients.consumers;

import com.jiefzz.ejoker.z.common.utilities.Ensure;
import com.jiefzz.ejoker.z.queue.IConsumer;

public abstract class AbstractConsumer implements IConsumer {

	protected IMessageHandler commandConsumer=null;
	
	@Override
	public IConsumer setMessageHandler(IMessageHandler commandConsumer) {
		Ensure.notNull(commandConsumer, "commandConsumer");
		this.commandConsumer = commandConsumer;
		return this;
	}
	
}
