package com.jiefzz.ejoker.queue.applicationMessage;

import java.nio.charset.Charset;

import org.apache.rocketmq.client.exception.MQClientException;

import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import com.jiefzz.ejoker.queue.ITopicProvider;
import com.jiefzz.ejoker.queue.QueueMessageTypeCode;
import com.jiefzz.ejoker.queue.SendQueueMessageService;
import com.jiefzz.ejoker.queue.completation.DefaultMQProducer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;

@EService
public class ApplicationMessagePublisher implements IMessagePublisher<IApplicationMessage>, IWorkerService {

	@Dependence
	private ITopicProvider<IApplicationMessage> messageTopicProvider;
	
	/**
	 * all command will send by this object.
	 */
	@Dependence
	private SendQueueMessageService sendQueueMessageService;
	
	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private EJokerAsyncHelper eJokerAsyncHelper;
	
	private DefaultMQProducer producer;

	public ApplicationMessagePublisher useProducer(DefaultMQProducer producer) {
		this.producer = producer;
		return this;
	}

	public DefaultMQProducer getProducer() {
		return producer;
	}
	
	@Override
	public ApplicationMessagePublisher start() {
		try {
			producer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public ApplicationMessagePublisher shutdown() {
		producer.shutdown();
		return this;
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> publishAsync(IApplicationMessage message) {
		EJokerQueueMessage queueMessage = createEQueueMessage(message);
		return sendQueueMessageService.sendMessageAsync(producer, queueMessage,
				((null == message.getRoutingKey()) ? message.getId() : message.getRoutingKey()), message.getId(), null);
	}

	private EJokerQueueMessage createEQueueMessage(IApplicationMessage message) {
		String topic = messageTopicProvider.getTopic(message);
		String data = jsonConverter.convert(message);
		return new EJokerQueueMessage(topic, QueueMessageTypeCode.ApplicationMessage.ordinal(),
				data.getBytes(Charset.forName("UTF-8")), message.getClass().getName());
	}
}
