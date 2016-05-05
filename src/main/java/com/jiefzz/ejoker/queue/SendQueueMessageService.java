package com.jiefzz.ejoker.queue;

import com.jiefzz.ejoker.infrastructure.UnimplementException;
import com.jiefzz.ejoker.infrastructure.queue.protocols.Message;

public class SendQueueMessageService {
	public void sendMessage(IProducer producer, Message message, String routingKey) {
		producer.sendMessage(message, routingKey);
    }
    public void sendMessageAsync(IProducer producer, Message message, String routingKey) {
    	throw new UnimplementException(this.getClass()+"sendMessageAsync");
    }
}
