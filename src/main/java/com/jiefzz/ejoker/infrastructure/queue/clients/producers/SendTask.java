package com.jiefzz.ejoker.infrastructure.queue.clients.producers;

import com.jiefzz.ejoker.infrastructure.queue.protocols.Message;
import com.jiefzz.ejoker.infrastructure.z.common.task.IAsyncTask;

public class SendTask implements IAsyncTask<SendResult> {

	String routingKey;
	
	public SendTask(Message message, String routingKey) {
		this.routingKey = routingKey;
	}

	@Override
	public SendResult call() throws Exception {
		return new SendResult(SendStatus.Success, null, null);
	}

}
