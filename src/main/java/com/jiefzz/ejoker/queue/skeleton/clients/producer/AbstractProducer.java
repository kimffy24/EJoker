package com.jiefzz.ejoker.queue.skeleton.clients.producer;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.queue.skeleton.IQueueProducerWokerService;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

public abstract class AbstractProducer implements IProducer {
	
	private final static Logger logger = LoggerFactory.getLogger(AbstractProducer.class);
	
	AsyncPool asyncPool = ThreadPoolMaster.getPoolInstance(this.getClass());

	protected abstract void produce(final String routingKey, final EJokerQueueMessage message) throws IOException;
	
	@Override
	public SendResult sendMessage(EJokerQueueMessage message, String routingKey) {
		try {
			Future<SendResult> sendMessageAsync = sendMessageAsync(message, routingKey);
			return sendMessageAsync.get(12000l, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			e.printStackTrace();
			return new SendResult(SendStatus.Failed, /*null, */e.getMessage());
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
						} catch (Exception e) {
							AbstractProducer.logger.error("Send message faile!!!, message context: \"{}\"", message.body);
							e.printStackTrace();
							return new SendResult(SendStatus.Failed, /*null, */IOException.class.getName() + ": " +e.getMessage());
						}
						return new SendResult(SendStatus.Success, /*null, */null);
					}

				}
		);
	}
	
	@Override
	public IProducer getProducer(){ return this; }

	@Override
	public IQueueProducerWokerService useProducer(IProducer producer) { return this; }
}
