package pro.jiefzz.ejoker.queue.publishableExceptions;

import java.nio.charset.Charset;
import java.util.Map;

import pro.jiefzz.ejoker.infrastructure.IMessagePublisher;
import pro.jiefzz.ejoker.infrastructure.ISequenceMessage;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.SendQueueMessageService;
import pro.jiefzz.ejoker.queue.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.aware.IProducerWrokerAware;
import pro.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionCodecHelper;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.service.IWorkerService;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.context.EJokerTaskAsyncHelper;

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
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	private IProducerWrokerAware producer;

	public PublishableExceptionPublisher useProducer(IProducerWrokerAware producer) {
		this.producer = producer;
		return this;
	}

	@Override
	public PublishableExceptionPublisher start() {
		try {
			producer.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public PublishableExceptionPublisher shutdown() {
		try {
			producer.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		final Map<String, String> serializableInfo = PublishableExceptionCodecHelper.serialize(exception);
		PublishableExceptionMessage pMsg = new PublishableExceptionMessage();
		{
			boolean isSequenceMessage = exception instanceof ISequenceMessage;
			pMsg.setUniqueId(exception.getId());
			pMsg.setAggregateRootTypeName(isSequenceMessage ? ((ISequenceMessage )exception).getAggregateRootTypeName() : null);
			pMsg.setAggregateRootId(isSequenceMessage ? ((ISequenceMessage )exception).getAggregateRootStringId() : null);
			pMsg.setSerializableInfo(serializableInfo);
			
		}
		String data = jsonConverter.convert(pMsg);
		// 按照eNode的匿名内部类写法如下，喜欢的话可以替换
//		String data = jsonConverter.convert(new PublishableExceptionMessage() {{
//			boolean isSequenceMessage = exception instanceof ISequenceMessage;
//			this.setUniqueId(exception.getId());
//			this.setAggregateRootTypeName(isSequenceMessage ? ((ISequenceMessage )exception).getAggregateRootTypeName() : null);
//			this.setAggregateRootId(isSequenceMessage ? ((ISequenceMessage )exception).getAggregateRootStringId() : null);
//			this.setSerializableInfo(serializableInfo);
//		}});
		return new EJokerQueueMessage(topic, QueueMessageTypeCode.ExceptionMessage.ordinal(),
				data.getBytes(Charset.forName("UTF-8")), typeNameProvider.getTypeName(exception.getClass()));
	}
}
