package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.IObjectProxy;
import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public interface ICommandAsyncHandlerProxy extends IObjectProxy {
	
	default public SystemFutureWrapper<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommandContext context, ICommand command) throws Exception { return null; };
	
}
