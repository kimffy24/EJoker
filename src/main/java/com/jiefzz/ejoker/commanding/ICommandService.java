package com.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

public interface ICommandService {

	public Future<AsyncTaskResultBase> sendAsync(final ICommand command);
//	public void send(final ICommand command);
//	
//	public CommandResult execute(final ICommand command, final int timeoutMillis);
//	public CommandResult execute(final ICommand command, final CommandReturnType commandReturnType, final int timeoutMillis);
	
	public Future<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command);
	public Future<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command, final CommandReturnType commandReturnType);
	
}
