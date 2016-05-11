package com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers;

import com.jiefzz.ejoker.queue.command.CommandConsumer;

/**
 * IConsumer is use to make sure that CommandConsumer
 * will handle message from queue.
 * @author JiefzzLon
 *
 */
public interface IConsumer {

	public IConsumer setMessageHandler(CommandConsumer commandConsumer);
	
}
