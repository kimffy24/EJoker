package com.jiefzz.ejoker.queue.adapter.redis;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import com.jiefzz.ejoker.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.common.task.AsyncPool;
import com.jiefzz.ejoker.infrastructure.common.task.IAsyncTask;
import com.jiefzz.ejoker.infrastructure.queue.clients.producers.IProducer;
import com.jiefzz.ejoker.infrastructure.queue.clients.producers.SendResult;
import com.jiefzz.ejoker.infrastructure.queue.clients.producers.SendStatus;
import com.jiefzz.ejoker.infrastructure.queue.protocols.Message;
import com.jiefzz.extension.infrastructure.IMessageQueue;

@EService
public class Producer implements IProducer {

	AsyncPool asyncPool = new AsyncPool();
	
	@Resource
	IMessageQueue messageQueue;

	@Override
	public SendResult sendMessage(Message message, String routingKey) {
		Future<SendResult> sendMessageAsync = sendMessageAsync(message, routingKey);
		try {
			return sendMessageAsync.get();
		} catch (Exception e) {
			e.printStackTrace();
			return new SendResult(SendStatus.Failed, null, e.getMessage());
		}
	}

	@Override
	public Future<SendResult> sendMessageAsync(Message message, String routingKey) {
		
		AsyncTask asyncTask = new AsyncTask(message, routingKey);
		return asyncPool.execute(asyncTask);
		
	}

	public class AsyncTask implements IAsyncTask<SendResult> {

		Message message;
		String routingKey;
		
		public AsyncTask(Message message, String routingKey) {
			this.message = message;
			this.routingKey = routingKey;
		}
		
		@Override
		public SendResult call() throws Exception {
			try {
				//System.out.println("Dispatch with route key: \""+routingKey+"\"");
				Producer.this.messageQueue.produce(message.topic, message.body);
				return new SendResult(SendStatus.Success, null, null);
			} catch ( Exception e ) {
				return new SendResult(SendStatus.Failed, null, e.getMessage());
			}
		}
		
	}
}
