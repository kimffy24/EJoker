package com.jiefzz.ejoker.queue;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.infrastructure.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.infrastructure.common.io.BaseAsyncTaskResult;
import com.jiefzz.ejoker.infrastructure.common.task.AsyncPool;
import com.jiefzz.ejoker.infrastructure.common.task.IAsyncTask;
import com.jiefzz.ejoker.infrastructure.queue.clients.producers.IProducer;
import com.jiefzz.ejoker.infrastructure.queue.protocols.Message;

public class SendQueueMessageService {
	
	private AsyncPool asyncPool = new AsyncPool();
	
	public void sendMessage(IProducer producer, Message message, String routingKey) {
		producer.sendMessage(message, routingKey);
	}
	
	public Future<BaseAsyncTaskResult> sendMessageAsync(IProducer producer, Message message, String routingKey) {
		AsyncTask task = new AsyncTask(producer, message, routingKey);
		Future<BaseAsyncTaskResult> execute = asyncPool.execute(task);
		return execute;
	}
	
	private class AsyncTask implements IAsyncTask<BaseAsyncTaskResult> {
		
		IProducer producer;
		Message message;
		String routingKey;
		
		public AsyncTask(IProducer producer, Message message, String routingKey) {
			this.producer = producer;
			this.message = message;
			this.routingKey = routingKey;
		}
		
		@Override
		public BaseAsyncTaskResult call() throws Exception {
			try {
				producer.sendMessageAsync(message, routingKey);
				return BaseAsyncTaskResult.Success;
			} catch ( Exception e ) {
				return new BaseAsyncTaskResult(AsyncTaskStatus.Failed, e.getMessage());
			}
		}
		
	}
}
