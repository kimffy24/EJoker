package com.jiefzz.ejoker.commanding;

public interface ICommandHandlerPrivider {
	
	public ICommandHandlerProxy getHandler(Class<? extends ICommand> commandType);
	
}
