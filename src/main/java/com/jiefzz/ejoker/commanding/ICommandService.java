package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public interface ICommandService {

	public SystemFutureWrapper<AsyncTaskResult<Void>> sendAsync(final ICommand command);
	
	public default SystemFutureWrapper<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command) {
		return executeAsync(command, CommandReturnType.CommandExecuted);
	}
	
	public SystemFutureWrapper<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command, final CommandReturnType commandReturnType);
	
}
