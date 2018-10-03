package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.IMessage;

public interface ICommand extends IMessage {

	public String getAggregateRootId();
	
}
