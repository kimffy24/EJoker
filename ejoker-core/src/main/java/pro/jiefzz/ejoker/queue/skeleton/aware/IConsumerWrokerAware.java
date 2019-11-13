package pro.jiefzz.ejoker.queue.skeleton.aware;

import pro.jiefzz.ejoker.z.system.functional.IVoidFunction2;

public interface IConsumerWrokerAware {

	public void start() throws Exception;

	public void shutdown() throws Exception;
	
	public void subscribe(String topic, String filter);
	
	public void registerEJokerCallback(IVoidFunction2<EJokerQueueMessage, IEJokerQueueMessageContext> vf);
	
	public void loopInterval();
	
}
