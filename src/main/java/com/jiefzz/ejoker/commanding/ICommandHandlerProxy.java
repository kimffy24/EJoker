package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface ICommandHandlerProxy extends IObjectProxy {
	
	public void handle(ICommandContext context, ICommand command);
	
}
