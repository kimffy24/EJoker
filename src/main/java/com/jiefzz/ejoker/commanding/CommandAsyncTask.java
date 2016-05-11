package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.io.BaseAsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.queue.clients.producers.IProducer;
import com.jiefzz.ejoker.z.queue.protocols.Message;

public class CommandAsyncTask implements IAsyncTask<BaseAsyncTaskResult> {

	private String routingKey;
	
	public CommandAsyncTask(IProducer producer, Message message, String routingKey) {
		this.routingKey = routingKey;
	}

	@Override
	public BaseAsyncTaskResult call() throws Exception {
		try {
			return BaseAsyncTaskResult.Success;
		} catch (Exception e) {
			return new BaseAsyncTaskResult(AsyncTaskStatus.Failed);
		}
	}

}
