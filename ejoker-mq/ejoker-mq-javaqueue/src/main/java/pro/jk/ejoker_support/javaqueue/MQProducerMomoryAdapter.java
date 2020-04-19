package pro.jk.ejoker_support.javaqueue;

import pro.jk.ejoker.common.system.enhance.MapUtilx;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jk.ejoker.queue.skeleton.aware.IProducerWrokerAware;

public class MQProducerMomoryAdapter implements ICQProvider, IProducerWrokerAware {

	@Override
	public void start() throws Exception {
	}

	@Override
	public void shutdown() throws Exception {
		
	}

	@Override
	public void send(EJokerQueueMessage message, String routingKey, ContextAware cxt) {
		MapUtilx.getOrAdd(mockMsgQueues, message.getTopic(), DSH::new).offer(message);
	}

}
