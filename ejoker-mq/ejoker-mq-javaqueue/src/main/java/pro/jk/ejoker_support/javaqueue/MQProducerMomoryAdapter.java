package pro.jk.ejoker_support.javaqueue;

import pro.jk.ejoker.common.system.enhance.MapUtilx;
import pro.jk.ejoker.common.system.functional.IVoidFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
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
	public void send(EJokerQueueMessage message, String routingKey, IVoidFunction successAction,
			IVoidFunction1<String> faildAction, IVoidFunction1<Exception> exceptionAction) {
		MapUtilx.getOrAdd(mockMsgQueues, message.getTopic(), DSH::new).offer(message);
	}

}
