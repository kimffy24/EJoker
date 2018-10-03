package com.jiefzz.ejoker.infrastructure.impl;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

public abstract class MessageHandlerA implements IMessageHandler {

	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message) {
		assert false;
		return null;
	}

}
