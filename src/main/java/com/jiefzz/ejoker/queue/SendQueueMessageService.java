package com.jiefzz.ejoker.queue;

import java.io.IOException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.queue.skeleton.clients.producer.IProducer;
import com.jiefzz.ejoker.queue.skeleton.clients.producer.SendResult;
import com.jiefzz.ejoker.queue.skeleton.clients.producer.SendStatus;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.io.IOExceptionOnRuntime;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

@EService
public class SendQueueMessageService {

	private final static Logger logger = LoggerFactory.getLogger(SendQueueMessageService.class);

	private AsyncPool asyncPool = ThreadPoolMaster.getPoolInstance(SendQueueMessageService.class);

	private final static String IOEXCEPTION_SIGN = IOException.class.getName();

	public void sendMessage(IProducer producer, EJokerQueueMessage message, String routingKey) {
		try {
			SendResult sendResult = producer.sendMessage(message, routingKey);
			if (SendStatus.Success != sendResult.sendStatus) {
				logger.error("Queue message sync send failed! [sendResult={}, routingKey={}]", sendResult, routingKey);
				throw new IOException(sendResult.errorMessage);
			}
			logger.debug("Queue message sync send succeed. [sendResult={}, routingKey={}]", sendResult, routingKey);
		} catch (Exception e) {
			logger.error(String.format("Queue message sync send has exception! [message=%s, routingKey=%s]",
					message.toString(), routingKey), e);
			throw IOExceptionOnRuntime.encapsulation(e);
		}

	}

	public Future<AsyncTaskResultBase> sendMessageAsync(IProducer producer, EJokerQueueMessage message, String routingKey) {
		Future<AsyncTaskResultBase> execute = asyncPool.execute(() -> {
				try {
					Future<SendResult> future = producer.sendMessageAsync(message, routingKey);
					SendResult sendResult = future.get();
					if (sendResult.errorMessage != null && sendResult.errorMessage.startsWith(IOEXCEPTION_SIGN)) {
						logger.error("EJoker message async send failed, sendResult: {}", sendResult.toString());
						return new AsyncTaskResultBase(AsyncTaskStatus.IOException, sendResult.errorMessage);
					}
					return AsyncTaskResultBase.Success;
				} catch (Exception e) {
					logger.error("EJoker message async send has exception.", e);
					return new AsyncTaskResultBase(AsyncTaskStatus.IOException, e.getMessage());
				}
		});
		return execute;
	}
}
