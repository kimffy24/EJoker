package com.jiefzz.ejoker.infrastructure;

import java.util.Collection;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;

public interface IMessageDispatcher {

	/**
	 * Dispatch the given message async.
	 * @param message
	 * @return
	 */
	SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessageAsync(IMessage message);
	
	/**
	 * Dispatch the given messages async.
	 * @param messages
	 * @return
	 */
	SystemFutureWrapper<AsyncTaskResult<Void>> dispatchMessagesAsync(Collection<? extends IMessage> messages);

}
