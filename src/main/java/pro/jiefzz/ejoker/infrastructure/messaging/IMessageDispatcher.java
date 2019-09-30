package pro.jiefzz.ejoker.infrastructure.messaging;

import java.util.Collection;
import java.util.concurrent.Future;

import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IMessageDispatcher {

	/**
	 * Dispatch the given message async.
	 * @param message
	 * @return
	 */
	Future<AsyncTaskResult<Void>> dispatchMessageAsync(IMessage message);
	
	/**
	 * Dispatch the given messages async.
	 * @param messages
	 * @return
	 */
	Future<AsyncTaskResult<Void>> dispatchMessagesAsync(Collection<? extends IMessage> messages);

}
