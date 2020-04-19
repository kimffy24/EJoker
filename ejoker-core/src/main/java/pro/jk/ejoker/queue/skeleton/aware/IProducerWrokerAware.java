package pro.jk.ejoker.queue.skeleton.aware;

public interface IProducerWrokerAware {

	public void start() throws Exception;

	public void shutdown() throws Exception;
	
	public void send(final EJokerQueueMessage message, final String routingKey, ContextAware cxt);
	
	public static interface ContextAware {
		
		public void triggerSuccess();
		
		public void triggerFaild(String reason);
		
		public void triggerException(Exception e);
		
	}
}
