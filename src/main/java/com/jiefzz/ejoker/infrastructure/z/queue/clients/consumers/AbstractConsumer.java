package com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers;

import com.jiefzz.ejoker.infrastructure.z.common.utilities.Ensure;
import com.jiefzz.ejoker.infrastructure.z.queue.IConsumer;

public abstract class AbstractConsumer implements IConsumer {

	protected IMessageHandler commandConsumer=null;
	
	@Override
	public IConsumer setMessageHandler(IMessageHandler commandConsumer) {
		Ensure.notNull(commandConsumer, "commandConsumer");
		this.commandConsumer = commandConsumer;
		return this;
	}
	
}
