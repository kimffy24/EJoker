package com.jiefzz.ejoker.queue.skeleton.clients.consumer;

import com.jiefzz.ejoker.queue.skeleton.IQueueComsumerWokerService;

/**
 * IConsumer is use to make sure that CommandConsumer
 * will handle message from queue.
 * @author JiefzzLon
 *
 */
public interface IConsumer extends IQueueComsumerWokerService {

	public IConsumer setMessageHandler(IMessageHandler commandConsumer);
	
}
