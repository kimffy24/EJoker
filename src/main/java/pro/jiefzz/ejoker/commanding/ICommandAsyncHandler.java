package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface ICommandAsyncHandler<TCommand extends ICommand> {

	public abstract SystemFutureWrapper<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommandContext context, TCommand command);
	
	public abstract SystemFutureWrapper<AsyncTaskResult<IApplicationMessage>> handleAsync(TCommand command);
	
}
