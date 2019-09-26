package pro.jiefzz.ejoker.queue.applicationMessage;

import java.nio.charset.Charset;

import pro.jiefzz.ejoker.infrastructure.IMessagePublisher;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.SendQueueMessageService;
import pro.jiefzz.ejoker.queue.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.aware.IProducerWrokerAware;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.service.IWorkerService;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.context.EJokerTaskAsyncHelper;

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
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	private IProducerWrokerAware producer;

	public ApplicationMessagePublisher useProducer(IProducerWrokerAware producer) {
		this.producer = producer;
		return this;
	}

	@Override
	public ApplicationMessagePublisher start() {
		try {
			producer.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public ApplicationMessagePublisher shutdown() {
		try {
			producer.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
				data.getBytes(Charset.forName("UTF-8")), typeNameProvider.getTypeName(message.getClass()));
	}
}
