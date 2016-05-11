package com.jiefzz.ejoker.infrastructure.z.queue;

import com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers.IMessageHandler;

/**
 * IConsumer is use to make sure that CommandConsumer
 * will handle message from queue.
 * @author JiefzzLon
 *
 */
public interface IConsumer extends IQueueWokerService {

	public IConsumer setMessageHandler(IMessageHandler commandConsumer);
	
}
