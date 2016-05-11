package com.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.BaseAsyncTaskResult;

public interface ICommandService {

	public Future<BaseAsyncTaskResult> sendAsync(ICommand command);
	public void send(ICommand command);
	
	public void execute(ICommand command, int timeoutMillis);
	public void execute(ICommand command, CommandReturnType commandReturnType, int timeoutMillis);
	
	public void executeAsync(ICommand command);
	public void executeAsync(ICommand command, CommandReturnType commandReturnType);
	
}
