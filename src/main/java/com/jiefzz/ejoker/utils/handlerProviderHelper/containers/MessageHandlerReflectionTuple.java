package com.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.EJoker;
import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

/**
 * message handlers record object
 * 
 * @author kimffy
 *
 */
public class MessageHandlerReflectionTuple implements IMessageHandlerProxy {
	
	public final Class<? extends IMessageHandler> handlerClass;
	public final Method handleReflectionMethod;
	public final String identification;

	private IMessageHandler handler = null;

	public MessageHandlerReflectionTuple(Class<? extends IMessageHandler> handlerClass, Method handleReflectionMethod) {
		this.handleReflectionMethod = handleReflectionMethod;
		this.handlerClass = handlerClass;
		Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
		identification = String.format("Proxy[ forward: %s#%s(%s) ]", handlerClass.getSimpleName(),
				MessageHandlerPool.HANDLER_METHOD_NAME, parameterTypes[0].getSimpleName());
	}

	@Override
	public IMessageHandler getInnerObject() {
		if (null == handler)
			handler = EJoker.getInstance().getEJokerContext().get(handlerClass);
		return handler;
	}

	@Override
	public Future<AsyncTaskResultBase> handleAsync(IMessage message) {
		try {
			return (Future<AsyncTaskResultBase>) handleReflectionMethod.invoke(getInnerObject(), message);
			// return getInnerObject().handleAsync(message);
		} catch (Exception e) {
			e.printStackTrace();
			throw new InfrastructureRuntimeException("Invoke handleAsync failed!!! " + message.toString(), e);
		}
	}

	@Override
	public String toString() {
		return identification;
	}
}
