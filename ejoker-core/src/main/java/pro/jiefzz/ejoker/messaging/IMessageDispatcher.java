package pro.jiefzz.ejoker.messaging;

import java.util.Collection;
import java.util.concurrent.Future;

public interface IMessageDispatcher {

	/**
	 * Dispatch the given message async.
	 * @param message
	 * @return
	 */
	Future<Void> dispatchMessageAsync(IMessage message);
	
	/**
	 * Dispatch the given messages async.
	 * @param messages
	 * @return
	 */
	Future<Void> dispatchMessagesAsync(Collection<? extends IMessage> messages);

}
