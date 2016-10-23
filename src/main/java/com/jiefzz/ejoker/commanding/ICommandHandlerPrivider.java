package com.jiefzz.ejoker.commanding;

public interface ICommandHandlerPrivider {
	
	public ICommandHandlerPrivider getHandler(Class<? extends ICommand> commandType);
	
}
