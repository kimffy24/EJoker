package com.jiefzz.ejoker.z.queue.clients.producers;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;
import com.jiefzz.ejoker.z.queue.IProducer;
import com.jiefzz.ejoker.z.queue.protocols.Message;

public abstract class AbstractProducer implements IProducer {
	
	final static Logger logger = LoggerFactory.getLogger(AbstractProducer.class);
	
	AsyncPool asyncPool = ThreadPoolMaster.getPoolInstance(AbstractProducer.class);

	//protected abstract void produce(String routingKey, String body) throws IOException;
	//protected abstract void produce(String routingKey, byte[] body) throws IOException;
	protected abstract void produce(String routingKey, Message message) throws IOException;
	
	@Override
	public SendResult sendMessage(Message message, String routingKey) {
		Future<SendResult> sendMessageAsync = sendMessageAsync(message, routingKey);
		try {
			return sendMessageAsync.get(12000, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			//e.printStackTrace();
			return new SendResult(SendStatus.Failed, null, e.getMessage());
		}
	}

	@Override
	public Future<SendResult> sendMessageAsync(Message message, String routingKey) {
		return asyncPool.execute(
				new IAsyncTask<SendResult>() {

					Message message;
					String routingKey;

					public IAsyncTask<SendResult> bind(Message message, String routingKey) {
						this.message = message;
						this.routingKey = routingKey;
						return this;
					}

					@Override
					public SendResult call() {
						try {
							AbstractProducer.this.produce(routingKey, message);
						} catch (IOException e) {
							AbstractProducer.this.logger.error("Send message faile!!!, message context: \"{}\"", message.body);
							e.printStackTrace();
							return new SendResult(SendStatus.Failed, null, IOException.class.getName() + ": " +e.getMessage());
						}
						return new SendResult(SendStatus.Success, null, null);
					}

				}.bind(message, routingKey)
		);
	}
}
