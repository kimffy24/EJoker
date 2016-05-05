package com.jiefzz.ejoker.infrastructure;

public interface IMessagePublisher {

	public void publishAsync(IMessage message);
	
}
