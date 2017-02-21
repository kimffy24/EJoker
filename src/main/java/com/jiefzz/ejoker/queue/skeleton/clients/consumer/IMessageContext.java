package com.jiefzz.ejoker.queue.skeleton.clients.consumer;

import com.jiefzz.ejoker.queue.skeleton.prototype.Message;

public interface IMessageContext {
	
	public void onMessageHandled(Message Message);
	
}
