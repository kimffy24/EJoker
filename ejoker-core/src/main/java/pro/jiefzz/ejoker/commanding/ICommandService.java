package pro.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.z.system.task.AsyncTaskResult;

public interface ICommandService {

	public Future<AsyncTaskResult<Void>> sendAsync(final ICommand command);
	
	public default Future<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command) {
		return executeAsync(command, CommandReturnType.CommandExecuted);
	}
	
	public Future<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command, final CommandReturnType commandReturnType);
	
}
