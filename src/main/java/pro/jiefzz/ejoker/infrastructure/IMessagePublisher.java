package pro.jiefzz.ejoker.infrastructure;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;

public interface IMessagePublisher<TMessage extends IMessage> {

	public SystemFutureWrapper<AsyncTaskResult<Void>> publishAsync(TMessage message);
	
}
