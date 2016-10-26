package com.jiefzz.ejoker.infrastructure;

public interface IMessagePublisher<TMessage extends IMessage> {

	public void publishAsync(TMessage message);
	
}
