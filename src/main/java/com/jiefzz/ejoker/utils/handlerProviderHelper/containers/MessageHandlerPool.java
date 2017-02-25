package com.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;

/**
 * 由于message类型可以有多个handler，
 * <br>所以用起来会有点复杂。
 * @author kimffy
 *
 */
public class MessageHandlerPool {

	public final static String HANDLER_METHOD_NAME = "handleAsync";

	private final static int PARAMETER_AMOUNT = 1;

	private final static Class<?> PARAMETER_TYPE_SUPER_0 = IMessage.class;

	private final static Lock lock4addNewTupleContainer = new ReentrantLock();

	private final static Map<Class<? extends IMessage>, List<MessageHandlerReflectionTuple>> handlerMapper =
			new ConcurrentHashMap<Class<? extends IMessage>, List<MessageHandlerReflectionTuple>>();

	public final static void regist(Class<? extends IMessageHandler> implementationHandlerClass) {
		final Method[] declaredMethods = implementationHandlerClass.getDeclaredMethods();
		for (int i = 0; i < declaredMethods.length; i++) {
			Method method = declaredMethods[i];
			if (!HANDLER_METHOD_NAME.equals(method.getName()))
				continue;
			if (method.getParameterCount() != PARAMETER_AMOUNT)
				continue;
			if (!method.isAccessible())
				method.setAccessible(true);
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (!PARAMETER_TYPE_SUPER_0.isAssignableFrom(parameterTypes[0]))
				throw new InfrastructureRuntimeException(String.format(
						"Type of %s#%s( %s ) first parameters is not accept!!!", implementationHandlerClass.getName(),
						HANDLER_METHOD_NAME, parameterTypes[0].getName()));
			Class<? extends IMessage> messageType = (Class<? extends IMessage>) parameterTypes[0];
			getOrAddNewElementContainer(messageType).add(new MessageHandlerReflectionTuple(implementationHandlerClass, method));
		}
	}

	public final static List<MessageHandlerReflectionTuple> getProxyAsyncHandlers(Class<? extends IMessage> messageType) {
		return getOrAddNewElementContainer(messageType);
	}
	
	private final static List<MessageHandlerReflectionTuple> getOrAddNewElementContainer(Class<? extends IMessage> messageType) {
		List<MessageHandlerReflectionTuple> containerList;
		if(null == (containerList = handlerMapper.getOrDefault(messageType, null))) {
			lock4addNewTupleContainer.lock();
			try {
				containerList = new ArrayList<MessageHandlerReflectionTuple>();
				handlerMapper.put(messageType, containerList);
				return containerList;
			} finally {
				lock4addNewTupleContainer.unlock();
			}
		} else
			return containerList;
	}

}
