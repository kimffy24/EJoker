package com.jiefzz.ejoker.commanding;

public interface ICommandRoutingKeyProvider {

	public String getRoutingKey(ICommand command);
	
}
