package com.jiefzz.ejoker.queue;

import java.io.IOException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.queue.skeleton.clients.producer.IProducer;
import com.jiefzz.ejoker.queue.skeleton.clients.producer.SendResult;
import com.jiefzz.ejoker.queue.skeleton.clients.producer.SendStatus;
import com.jiefzz.ejoker.queue.skeleton.prototype.Message;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.io.BaseAsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.IOExceptionOnRuntime;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

@EService
public class SendQueueMessageService {
	
	private final static Logger logger = LoggerFactory.getLogger(SendQueueMessageService.class);
	
	private AsyncPool asyncPool = ThreadPoolMaster.getPoolInstance(SendQueueMessageService.class);
	
	private final static String IOEXCEPTION_SIGN = IOException.class.getName();
	
	public void sendMessage(IProducer producer, Message message, String routingKey) {
		try {
			SendResult sendResult = producer.sendMessage(message, routingKey);
			if(SendStatus.Success != sendResult.sendStatus) {
				logger.error("Queue message sync send failed! [sendResult={}, routingKey={}]", sendResult, routingKey);
				throw new IOException(sendResult.errorMessage);
			}
			logger.debug("Queue message sync send succeed. [sendResult={}, routingKey={}]", sendResult, routingKey);
		} catch(Exception e) {
			logger.error(
					String.format("Queue message sync send has exception! [message=%s, routingKey=%s]", message.toString(), routingKey),
					e
			);
			throw IOExceptionOnRuntime.encapsulation(e);
		}

	}
		
	public Future<BaseAsyncTaskResult> sendMessageAsync(IProducer producer, Message message, String routingKey) {
		Future<BaseAsyncTaskResult> execute = asyncPool.execute(
				new IAsyncTask<BaseAsyncTaskResult>() {
					
					IProducer producer;
					Message message;
					String routingKey;
					
					public IAsyncTask<BaseAsyncTaskResult> bind(IProducer producer, Message message, String routingKey) {
						this.producer = producer;
						this.message = message;
						this.routingKey = routingKey;
						return this;
					}
					
					@Override
					public BaseAsyncTaskResult call() throws Exception {
						try {
							Future<SendResult> future = producer.sendMessageAsync(message, routingKey);
							SendResult sendResult = future.get();
							if(sendResult.errorMessage!=null && sendResult.errorMessage.startsWith(IOEXCEPTION_SIGN)) {
								logger.error(String.format("EJoker message async send failed, sendResult: %s", sendResult.toString()));
								return new BaseAsyncTaskResult(AsyncTaskStatus.IOException, sendResult.errorMessage);
							}
							return BaseAsyncTaskResult.Success;
						} catch ( Exception e ) {
							logger.error(String.format("EJoker message async send has exception."), e);
							return new BaseAsyncTaskResult(AsyncTaskStatus.IOException, e.getMessage());
						}
					}
					
				}.bind(producer, message, routingKey)
		);
		return execute;
	}
}
