package pro.jiefzz.ejoker.commanding;

import pro.jiefzz.ejoker.infrastructure.messaging.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

/**
 * Java could not multi-implement ICommandHandler.<br>
 * 使用直接重载。<br>
 * 同时在此处分析Command对应的CommandHandler<br>
 * @author jiefzz
 *
 */
public abstract class AbstractCommandHandler implements ICommandHandler<ICommand>, ICommandAsyncHandler<ICommand> {
	
	@Override
	public void handle(ICommandContext context, ICommand command) {
		throw new CommandRuntimeException("Do you forget to implement the handler function to handle command which type is " +command.getClass().getName());
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommandContext context, ICommand command) {
		throw new CommandRuntimeException("Do you forget to implement the async handler function to handle command which type is " +command.getClass().getName());
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommand command) {
		throw new CommandRuntimeException("Do you forget to implement the async handler function to handle command which type is " +command.getClass().getName());
	}
	
}
