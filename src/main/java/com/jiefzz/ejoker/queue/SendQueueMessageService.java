package com.jiefzz.ejoker.queue;

import java.util.concurrent.Future;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.queue.completation.DefaultMQProducer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;

@EService
public class SendQueueMessageService {

	private final static Logger logger = LoggerFactory.getLogger(SendQueueMessageService.class);

	private AsyncPool asyncPool = ThreadPoolMaster.getPoolInstance(SendQueueMessageService.class);

	public Future<AsyncTaskResultBase> sendMessageAsync(final DefaultMQProducer producer, final EJokerQueueMessage message,
			final String routingKey) {
		
		Future<AsyncTaskResultBase> execute = asyncPool.execute(() -> {
				try {
					SendResult sendResult = producer.send(new Message(message.getTopic(), message.getTag(), routingKey, message.getCode(), message.getBody(), true));
//					SendResult sendResult = future.get();
//					if (sendResult.errorMessage != null && sendResult.errorMessage.startsWith(IOEXCEPTION_SIGN)) {
					if(!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
						logger.error("EJoker message async send failed, sendResult: {}", sendResult.toString());
						return new AsyncTaskResultBase(AsyncTaskStatus.IOException, /*sendResult.errorMessage*/sendResult.getSendStatus().toString());
					}
					return AsyncTaskResultBase.Success;
				} catch (Exception e) {
					logger.error("EJoker message async send has exception.", e);
					return new AsyncTaskResultBase(AsyncTaskStatus.IOException, e.getMessage());
				}
			}

		);
		return execute;
	}
}
