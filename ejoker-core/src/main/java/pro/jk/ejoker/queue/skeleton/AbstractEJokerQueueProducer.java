package pro.jk.ejoker.queue.skeleton;

import java.util.concurrent.Future;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.service.IWorkerService;
import pro.jk.ejoker.messaging.IMessage;
import pro.jk.ejoker.messaging.IMessagePublisher;
import pro.jk.ejoker.queue.SendQueueMessageService;
import pro.jk.ejoker.queue.SendQueueMessageService.SendServiceContext;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jk.ejoker.queue.skeleton.aware.IProducerWrokerAware;

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
	public Future<Void> publishAsync(TMessage message) {
//		return sendQueueMessageService.sendMessageAsync(
//				producer,
//				this.getMessageType(message),
//				this.getMessageClassDesc(message),
//				this.createEQueueMessage(message),
//				this.getRoutingKey(message),
//				message.getId(),
//				message.getItems());
		return sendQueueMessageService.sendMessageAsync(
				producer,
				this.createEQueueMessage(message));
	}
	
	@Override
	public boolean isAllReady() {
		return true;
	}

	abstract protected String getMessageType(TMessage message);
	
	abstract protected String getRoutingKey(TMessage message);
	
	abstract protected SendServiceContext createEQueueMessage(TMessage message);
	
	protected String getMessageClassDesc(TMessage message) {
		return message.getClass().getSimpleName();
	}
	
}
