package pro.jiefzz.ejoker.queue.skeleton;

import pro.jiefzz.ejoker.infrastructure.messaging.IMessage;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessagePublisher;
import pro.jiefzz.ejoker.queue.SendQueueMessageService;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IProducerWrokerAware;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.service.IWorkerService;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public abstract class AbstractEJokerQueueProducer<TMessage extends IMessage> implements IMessagePublisher<TMessage>, IWorkerService {

	/**
	 * all command will send by this object.
	 */
	@Dependence
	protected SendQueueMessageService sendQueueMessageService;

	private IProducerWrokerAware producer;

	public AbstractEJokerQueueProducer<TMessage> useProducer(IProducerWrokerAware producer) {
		this.producer = producer;
		return this;
	}

	@Override
	public AbstractEJokerQueueProducer<TMessage> start() {
		try {
			producer.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public AbstractEJokerQueueProducer<TMessage> shutdown() {
		try {
			producer.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> publishAsync(TMessage message) {
		return sendQueueMessageService.sendMessageAsync(
				producer,
				this.getMessageType(message),
				this.getMessageClassDesc(message),
				this.createEQueueMessage(message),
				this.getRoutingKey(message),
				message.getId(),
				message.getItems());
	}
	
	abstract protected String getMessageType(TMessage message);
	
	abstract protected String getRoutingKey(TMessage message);
	
	abstract protected EJokerQueueMessage createEQueueMessage(TMessage message);
	
	protected String getMessageClassDesc(TMessage message) {
		return message.getClass().getSimpleName();
	}
	
}
