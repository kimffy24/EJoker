package pro.jiefzz.ejoker.messaging;

import java.util.concurrent.Future;

public interface IMessagePublisher<TMessage extends IMessage> {

	public Future<Void> publishAsync(TMessage message);
	
}
