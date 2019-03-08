package com.jiefzz.ejoker.queue;

import java.io.IOException;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.queue.completation.DefaultMQProducer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.IOExceptionOnRuntime;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;

@EService
public class SendQueueMessageService {

	private final static Logger logger = LoggerFactory.getLogger(SendQueueMessageService.class);

	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;

	public SystemFutureWrapper<AsyncTaskResult<Void>> sendMessageAsync(DefaultMQProducer producer,
			EJokerQueueMessage message, String routingKey, String messageId, String version) {

		Message rMessage = new Message(message.getTopic(), message.getTag(), routingKey, message.getCode(),
				message.getBody(), true);

		if (EJokerEnvironment.ASYNC_EJOKER_MESSAGE_SEND) {

			// use producer inner executor service to execute aSync task and wrap the result
			// with type SystemFutureWrapper.
			return new SystemFutureWrapper<>(producer.submitWithInnerExector(() -> {
				try {
					sendSync(producer, rMessage, messageId, version);
					return AsyncTaskResult.Success;
				} catch (Exception e) {
					return new AsyncTaskResult<>(AsyncTaskStatus.IOException, e.getMessage(), null);
				}
			}));

		} else {

			// use eJoker inner executor service
			return eJokerAsyncHelper.submit(() -> sendSync(producer, rMessage, messageId, version));
			
		}

	}

	private void sendSync(DefaultMQProducer producer, Message rMessage, String messageId, String version) {

		SendResult sendResult;
		try {
			sendResult = producer.send(rMessage);

			if (!SendStatus.SEND_OK.equals(sendResult.getSendStatus())) {
				logger.error(
						"EJoker message async send failed, sendResult: {}, routingKey: {}, messageId: {}, version: {}",
						sendResult.toString(), rMessage.getKeys(), messageId, version);
				throw new IOExceptionOnRuntime(new IOException(sendResult.toString()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(
					"EJoker message async send failed, message: {}, routingKey: {}, messageId: {}, version: {}",
					e.getMessage(), rMessage.getKeys(), messageId, version);
			throw new IOExceptionOnRuntime(new IOException(e));
		}
		
	}

}
