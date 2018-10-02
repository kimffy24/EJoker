package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public interface ICommandService {

	public SystemFutureWrapper<AsyncTaskResult<Void>> sendAsync(final ICommand command);
	
	public SystemFutureWrapper<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command);
	
	public SystemFutureWrapper<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command, final CommandReturnType commandReturnType);
	
}
