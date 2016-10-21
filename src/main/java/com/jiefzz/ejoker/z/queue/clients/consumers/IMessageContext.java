package com.jiefzz.ejoker.z.queue.clients.consumers;

import com.jiefzz.ejoker.z.queue.protocols.Message;

public interface IMessageContext {
	
	public void onMessageHandled(Message Message);
	
}
