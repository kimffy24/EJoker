package pro.jiefzz.ejoker.queue.skeleton.aware;

public interface IEJokerQueueMessageContext {
	
	public void onMessageHandled(EJokerQueueMessage queueMessage);
	
}
