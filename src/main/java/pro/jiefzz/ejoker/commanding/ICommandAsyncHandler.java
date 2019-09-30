package pro.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.infrastructure.messaging.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface ICommandAsyncHandler<TCommand extends ICommand> {

	public abstract Future<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommandContext context, TCommand command);
	
	public abstract Future<AsyncTaskResult<IApplicationMessage>> handleAsync(TCommand command);
	
}
