package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface ICommandHandlerProxy extends IObjectProxy {
	
	public void hadler(ICommandContext context, ICommand command);
	
}
