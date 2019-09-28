package pro.jiefzz.ejoker.infrastructure.impl;

import pro.jiefzz.ejoker.infrastructure.messaging.IMessage;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessageHandler;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public abstract class AbstractMessageHandler implements IMessageHandler {

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message) {
		assert false;
		return null;
	}

}
