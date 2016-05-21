package com.jiefzz.ejoker.z.queue.clients.producers;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;
import com.jiefzz.ejoker.z.queue.IProducer;
import com.jiefzz.ejoker.z.queue.protocols.Message;

@EService
public abstract class AbstractProducer implements IProducer {

	AsyncPool asyncPool = ThreadPoolMaster.getPoolInstance(AbstractProducer.class);

	@Resource
	IMessageProducer messageQueue;

	@Override
	public SendResult sendMessage(Message message, String routingKey) {
		Future<SendResult> sendMessageAsync = sendMessageAsync(message, routingKey);
		try {
			return sendMessageAsync.get(12000, TimeUnit.MILLISECONDS);
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
		public SendResult call() {
			// TODO logger!
			//System.out.println("Dispatch with route key: \""+routingKey+"\"");
			try {
				AbstractProducer.this.messageQueue.produce(routingKey, message.body);
				AbstractProducer.this.messageQueue.onProducerThreadClose();
			} catch (IOException e) {
				e.printStackTrace();
				return new SendResult(SendStatus.Failed, null, IOException.class.getName() + ": " +e.getMessage());
			}
			return new SendResult(SendStatus.Success, null, null);

		}

	}

}
