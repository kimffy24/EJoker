package com.jiefzz.ejoker.infrastructure;

import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.lambdaSupport.QIFunction;

public interface IMessageHandlerProxy extends IObjectProxy {
	
	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message);
	
	SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message,
			IFunction1<SystemFutureWrapper<AsyncTaskResult<Void>>, QIFunction<AsyncTaskResult<Void>>> submitter);
	
}
