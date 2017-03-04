package com.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.EJoker;
import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;

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
			if(null==parameterTypes || 1!=parameterTypes.length)
				throw new RuntimeException(String.format("Parameter signature of %s#%s is not accept!!!", implementationHandlerClass.getName(), method.getName()));
			if (!PARAMETER_TYPE_SUPER_0.isAssignableFrom(parameterTypes[0]))
				throw new InfrastructureRuntimeException(String.format(
						"%s#%s( %s ) first parameters is not accept!!!", implementationHandlerClass.getName(),
						HANDLER_METHOD_NAME, parameterTypes[0].getName()));
			Class<? extends IMessage> messageType = (Class<? extends IMessage> )parameterTypes[0];
			getOrAddNewElementContainer(messageType).add(new MessageHandlerReflectionTuple(method));
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
				// 当执行递归调用时，逻辑上能保证线程不会再进入临界区。
				if(handlerMapper.containsKey(messageType))
					return getOrAddNewElementContainer(messageType);
				containerList = new ArrayList<MessageHandlerReflectionTuple>();
				handlerMapper.put(messageType, containerList);
				return containerList;
			} finally {
				lock4addNewTupleContainer.unlock();
			}
		} else
			return containerList;
	}
	
	public static class MessageHandlerReflectionTuple implements IMessageHandlerProxy {
		
		public final Class<? extends IMessageHandler> handlerClass;
		public final Method handleReflectionMethod;
		public final String identification;

		private IMessageHandler handler = null;

		/**
		 * EJoker context是严格禁止同事获取对象的
		 * <br>用于在判断handler为空需要从上下中获取Handler的时候排他执行。
		 */
		private final static Lock lock4getHandler = new ReentrantLock();

		public MessageHandlerReflectionTuple(Method handleReflectionMethod) {
			this.handleReflectionMethod = handleReflectionMethod;
			this.handlerClass = (Class<? extends IMessageHandler> )handleReflectionMethod.getDeclaringClass();
			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
			identification = String.format("Proxy[ forward: %s#%s(%s) ]", handlerClass.getSimpleName(),
					MessageHandlerPool.HANDLER_METHOD_NAME, parameterTypes[0].getSimpleName());
		}

		@Override
		public IMessageHandler getInnerObject() {
			if (null == handler) {
				lock4getHandler.lock();
				try {
					if (null == handler) {
						handler = EJoker.getInstance().getEJokerContext().get(handlerClass);
						return handler;
					} else
						// 当执行递归调用时，逻辑上能保证线程不会再进入临界区。
						return getInnerObject();
				} finally {
					lock4getHandler.unlock();
				}
			} else
				return handler;
		}

		@Override
		public Future<AsyncTaskResultBase> handleAsync(IMessage message) {
			try {
				return (Future<AsyncTaskResultBase> )handleReflectionMethod.invoke(getInnerObject(), message);
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

}
