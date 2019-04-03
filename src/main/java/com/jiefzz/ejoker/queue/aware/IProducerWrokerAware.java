package com.jiefzz.ejoker.queue.aware;

public interface IProducerWrokerAware {

	public void start() throws Exception;

	public void shutdown() throws Exception;
	
	public void send(final EJokerQueueMessage message, final String routingKey, final String messageId, final String version);
	
}
