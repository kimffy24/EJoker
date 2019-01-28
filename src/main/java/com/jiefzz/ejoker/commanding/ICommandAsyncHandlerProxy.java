package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface ICommandAsyncHandlerProxy extends IObjectProxy {
	
	default public Object handleAsync(ICommandContext context, ICommand command) throws Exception { return null; };
	
}
