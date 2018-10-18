package com.jiefzz.ejoker.queue;

import java.io.IOException;

import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.queue.completation.DefaultMQProducer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;

@EService
public class SendQueueMessageService {

	private final static Logger logger = LoggerFactory.getLogger(SendQueueMessageService.class);

	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;

	public SystemFutureWrapper<AsyncTaskResult<Void>> sendMessageAsync(final DefaultMQProducer producer, final EJokerQueueMessage message,
			final String routingKey, String messageId, String version) {
		return eJokerAsyncHelper.submit(() -> {
					SendResult sendResult = producer.send(new Message(message.getTopic(), message.getTag(), routingKey, message.getCode(), message.getBody(), true));
					if(!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
						logger.error(
								"EJoker message async send failed, sendResult: {}, routingKey: {}, messageId: {}, version: {}",
								sendResult.toString(),
								routingKey,
								messageId,
								version
								);
						throw new IOException(sendResult.toString());
					}
			}
		);
	}

}
