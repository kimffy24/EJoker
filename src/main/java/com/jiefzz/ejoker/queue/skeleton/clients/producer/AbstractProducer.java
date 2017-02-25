package com.jiefzz.ejoker.queue.skeleton.clients.producer;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.queue.skeleton.IQueueProducerWokerService;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

public abstract class AbstractProducer implements IProducer {
	
	final static Logger logger = LoggerFactory.getLogger(AbstractProducer.class);
	
	AsyncPool asyncPool = ThreadPoolMaster.getPoolInstance(AbstractProducer.class);

	protected abstract void produce(String routingKey, EJokerQueueMessage message) throws IOException;
	
	@Override
	public SendResult sendMessage(EJokerQueueMessage message, String routingKey) {
		Future<SendResult> sendMessageAsync = sendMessageAsync(message, routingKey);
		try {
			return sendMessageAsync.get(12000, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			e.printStackTrace();
			return new SendResult(SendStatus.Failed, null, e.getMessage());
		}
	}

	@Override
	public Future<SendResult> sendMessageAsync(final EJokerQueueMessage message, final String routingKey) {
		return asyncPool.execute(
				new IAsyncTask<SendResult>() {
					@Override
					public SendResult call() {
						try {
							AbstractProducer.this.produce(routingKey, message);
						} catch (IOException e) {
							AbstractProducer.logger.error("Send message faile!!!, message context: \"{}\"", message.body);
							e.printStackTrace();
							return new SendResult(SendStatus.Failed, null, IOException.class.getName() + ": " +e.getMessage());
						}
						return new SendResult(SendStatus.Success, null, null);
					}

				}
		);
	}

	public IProducer getProducer(){ return this; }
	public IQueueProducerWokerService useProducer(IProducer producer) { return this; }
}
