package pro.jiefzz.ejoker.infrastructure.impl;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.messaging.IMessage;
import pro.jiefzz.ejoker.messaging.IMessageHandler;
import pro.jiefzz.ejoker.z.system.task.AsyncTaskResult;

public abstract class AbstractMessageHandler implements IMessageHandler {

	@Override
	public Future<AsyncTaskResult<Void>> handleAsync(IMessage message) {
		assert false;
		return null;
	}

}
