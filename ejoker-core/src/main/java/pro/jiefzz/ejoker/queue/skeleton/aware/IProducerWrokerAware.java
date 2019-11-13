package pro.jiefzz.ejoker.queue.skeleton.aware;

import pro.jiefzz.ejoker.z.system.functional.IVoidFunction;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction1;

public interface IProducerWrokerAware {

	public void start() throws Exception;

	public void shutdown() throws Exception;
	
	public void send(final EJokerQueueMessage message, final String routingKey, IVoidFunction successAction, IVoidFunction1<String> faildAction, IVoidFunction1<Exception> exceptionAction);
	
}
