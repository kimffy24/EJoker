package pro.jiefzz.ejoker.messaging;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface IMessageHandlerProxy extends IObjectProxy {

	Future<Void> handleAsync(IMessage... messages);
	
}
