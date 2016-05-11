package com.jiefzz.ejoker.queue;

import java.io.IOException;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.context.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.infrastructure.z.common.io.BaseAsyncTaskResult;
import com.jiefzz.ejoker.infrastructure.z.common.task.AsyncPool;
import com.jiefzz.ejoker.infrastructure.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.infrastructure.z.queue.clients.producers.IProducer;
import com.jiefzz.ejoker.infrastructure.z.queue.clients.producers.SendResult;
import com.jiefzz.ejoker.infrastructure.z.queue.protocols.Message;

@EService
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
				Future<SendResult> future = producer.sendMessageAsync(message, routingKey);
				SendResult sendResult = future.get();
				if(sendResult.errorMessage!=null && IOException.class.getName().equals(sendResult.errorMessage))
					return new BaseAsyncTaskResult(AsyncTaskStatus.IOException);
				return BaseAsyncTaskResult.Success;
			} catch ( Exception e ) {
				return new BaseAsyncTaskResult(AsyncTaskStatus.Failed, e.getMessage());
			}
		}
		
	}
}
