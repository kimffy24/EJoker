package pro.jk.ejoker.queue.skeleton.aware;

public interface IEJokerQueueMessageContext {
	
	public void onMessageHandled(EJokerQueueMessage queueMessage);
	
}
