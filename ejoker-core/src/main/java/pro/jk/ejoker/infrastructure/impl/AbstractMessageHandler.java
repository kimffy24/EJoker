package pro.jk.ejoker.infrastructure.impl;

import java.util.concurrent.Future;

import pro.jk.ejoker.messaging.IMessage;
import pro.jk.ejoker.messaging.IMessageHandler;

public abstract class AbstractMessageHandler implements IMessageHandler {

	@Override
	public Future<Void> handleAsync(IMessage message) {
		assert false;
		return null;
	}

}
