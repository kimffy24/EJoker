package com.jiefzz.ejoker.infrastructure.common.task;

import com.jiefzz.ejoker.infrastructure.queue.protocols.Message;
import com.jiefzz.ejoker.queue.IProducer;

public abstract class AbstractAsyncTask implements IAsyncTask {

	protected IProducer producer;
	protected Message message;
	
	public AbstractAsyncTask(IProducer producer, Message message) {
		this.producer = producer;
		this.message = message;
	}

}
