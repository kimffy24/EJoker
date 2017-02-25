package com.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

public interface ICommandService {

	public Future<AsyncTaskResultBase> sendAsync(ICommand command);
	public void send(ICommand command);
	
	public CommandResult execute(ICommand command, int timeoutMillis);
	public CommandResult execute(ICommand command, CommandReturnType commandReturnType, int timeoutMillis);
	
	public Future<AsyncTaskResult<CommandResult>> executeAsync(ICommand command);
	public Future<AsyncTaskResult<CommandResult>> executeAsync(ICommand command, CommandReturnType commandReturnType);
	
}
