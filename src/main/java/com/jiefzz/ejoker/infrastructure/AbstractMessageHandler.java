package com.jiefzz.ejoker.infrastructure;

import java.util.concurrent.Future;

import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

public abstract class AbstractMessageHandler implements IMessageHandler {

	@Override
	public Future<AsyncTaskResultBase> handleAsync(IMessage message) {
		assert false;
		return null;
	}

}
