package com.jiefzz.ejoker.queue.skeleton.clients.consumer;

import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;

public interface IEJokerQueueMessageContext {
	
	public void onMessageHandled(EJokerQueueMessage Message);
	
}
