package pro.jk.ejoker.messaging;

import java.util.concurrent.Future;

import pro.jk.ejoker.infrastructure.IObjectProxy;

public interface IMessageHandlerProxy extends IObjectProxy {

	Future<Void> handleAsync(IMessage... messages);
	
}
