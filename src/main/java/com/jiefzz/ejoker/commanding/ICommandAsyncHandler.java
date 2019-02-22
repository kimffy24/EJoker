package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public interface ICommandAsyncHandler<TCommand extends ICommand> {

	public abstract SystemFutureWrapper<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommandContext context, TCommand command);
	
	public abstract SystemFutureWrapper<AsyncTaskResult<IApplicationMessage>> handleAsync(TCommand command);
	
}
