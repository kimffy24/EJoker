package com.jiefzz.ejoker.infrastructure;

import java.util.List;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

public interface IMessageDispatcher {

	/**
	 * Dispatch the given message async.
	 * @param message
	 * @return
	 */
	Future<AsyncTaskResultBase> dispatchMessageAsync(IMessage message);
	
	/**
	 * Dispatch the given messages async.
	 * @param messages
	 * @return
	 */
	Future<AsyncTaskResultBase> dispatchMessagesAsync(List<IMessage> messages);
    
}
