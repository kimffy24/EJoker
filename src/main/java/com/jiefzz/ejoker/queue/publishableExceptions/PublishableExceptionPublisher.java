package com.jiefzz.ejoker.queue.publishableExceptions;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.rocketmq.client.exception.MQClientException;

import com.jiefzz.ejoker.infrastructure.IMessagePublisher;
import com.jiefzz.ejoker.infrastructure.ISequenceMessage;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import com.jiefzz.ejoker.queue.ITopicProvider;
import com.jiefzz.ejoker.queue.QueueMessageTypeCode;
import com.jiefzz.ejoker.queue.SendQueueMessageService;
import com.jiefzz.ejoker.queue.completation.DefaultMQProducer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionHelper;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;

@EService
public class PublishableExceptionPublisher implements IMessagePublisher<IPublishableException>, IWorkerService {

	@Dependence
	private ITopicProvider<IPublishableException> messageTopicProvider;
	
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

	public PublishableExceptionPublisher useProducer(DefaultMQProducer producer) {
		this.producer = producer;
		return this;
	}

	public DefaultMQProducer getProducer() {
		return producer;
	}
	
	@Override
	public PublishableExceptionPublisher start() {
		try {
			producer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public PublishableExceptionPublisher shutdown() {
		producer.shutdown();
		return this;
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> publishAsync(IPublishableException exception) {
		EJokerQueueMessage queueMessage = createEQueueMessage(exception);
		return sendQueueMessageService.sendMessageAsync(producer, queueMessage,
				((null == exception.getRoutingKey()) ? exception.getId() : exception.getRoutingKey()), exception.getId(), null);
	}

	private EJokerQueueMessage createEQueueMessage(IPublishableException exception) {
		String topic = messageTopicProvider.getTopic(exception);
		final Map<String, String> serializableInfo = PublishableExceptionHelper.serialize(exception);
		String data = jsonConverter.convert(new PublishableExceptionMessage() {{
			boolean isSequenceMessage = exception instanceof ISequenceMessage;
			this.setUniqueId(exception.getId());
			this.setAggregateRootTypeName(isSequenceMessage ? ((ISequenceMessage )exception).getAggregateRootTypeName() : null);
			this.setAggregateRootId(isSequenceMessage ? ((ISequenceMessage )exception).getAggregateRootStringId() : null);
			this.setSerializableInfo(serializableInfo);
		}});
		return new EJokerQueueMessage(topic, QueueMessageTypeCode.ExceptionMessage.ordinal(),
				data.getBytes(Charset.forName("UTF-8")), exception.getClass().getName());
	}
}
