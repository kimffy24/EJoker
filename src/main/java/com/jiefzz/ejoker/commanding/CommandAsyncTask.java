package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.infrastructure.common.io.BaseAsyncTaskResult;
import com.jiefzz.ejoker.infrastructure.common.task.AbstractAsyncTask;
import com.jiefzz.ejoker.infrastructure.queue.protocols.Message;
import com.jiefzz.ejoker.queue.IProducer;

public class CommandAsyncTask extends AbstractAsyncTask {

	private String routingKey;
	
	public CommandAsyncTask(IProducer producer, Message message, String routingKey) {
		super(producer, message);
		this.routingKey = routingKey;
	}

	@Override
	public BaseAsyncTaskResult call() throws Exception {
		try {
			producer.sendMessage(message, routingKey);
			return BaseAsyncTaskResult.Success;
		} catch (Exception e) {
			return new BaseAsyncTaskResult(AsyncTaskStatus.Failed);
		}
	}

}
