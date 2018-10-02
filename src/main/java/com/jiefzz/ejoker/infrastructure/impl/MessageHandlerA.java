package com.jiefzz.ejoker.infrastructure.impl;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

public abstract class MessageHandlerA implements IMessageHandler {

	@Override
	public Future<AsyncTaskResultBase> handleAsync(IMessage message) {
		assert false;
		return null;
	}

}
