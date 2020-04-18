package pro.jk.ejoker.messaging;

import java.util.concurrent.Future;

public interface IMessageHandler {

	Future<Void> handleAsync(IMessage message);
	
}
