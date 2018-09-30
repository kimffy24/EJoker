package com.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.jiefzz.ejoker.EJoker;
import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;

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

	private final static Map<Class<? extends IMessage>, List<MessageHandlerReflectionTuple>> handlerMapper = new ConcurrentHashMap<>();

	public final static void regist(Class<? extends IMessageHandler> implementationHandlerClazz) {
		
		Set<String> coverSet = new HashSet<>();
		String actuallyHandlerName = implementationHandlerClazz.getName();
		for(Class<?> clazz = implementationHandlerClazz; !Object.class.equals(clazz) && null != clazz; clazz = clazz.getSuperclass() ) {
			final Method[] declaredMethods = clazz.getDeclaredMethods();
			for (int i = 0; i < declaredMethods.length; i++) {
				Method method = declaredMethods[i];
				if (!HANDLER_METHOD_NAME.equals(method.getName()))
					continue;
				if (method.getParameterCount() != PARAMETER_AMOUNT)
					throw new RuntimeException(String.format("Parameter signature of %s#%s is not accept!!!", actuallyHandlerName, method.getName()));
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (!PARAMETER_TYPE_SUPER_0.isAssignableFrom(parameterTypes[0]))
					throw new InfrastructureRuntimeException(String.format(
							"%s#%s( %s ) first parameters is not accept!!!", actuallyHandlerName,
							HANDLER_METHOD_NAME, parameterTypes[0].getName()));
				Class<? extends IMessage> messageType = (Class<? extends IMessage> )parameterTypes[0];
				if(coverSet.contains(messageType.getName()))
					continue;
				coverSet.add(messageType.getName());
				if (!method.isAccessible())
					method.setAccessible(true);
				List<MessageHandlerReflectionTuple> handlerInvokerList = MapHelper.getOrAdd(handlerMapper, messageType, () -> new ArrayList<>());
				handlerInvokerList.add(new MessageHandlerReflectionTuple(method));
			}
		}
		
	}

	public final static List<MessageHandlerReflectionTuple> getProxyAsyncHandlers(Class<? extends IMessage> messageType) {
		return MapHelper.getOrAdd(handlerMapper, messageType, () -> new ArrayList<>());
	}
	
	public static class MessageHandlerReflectionTuple implements IMessageHandlerProxy {
		
		public final Class<? extends IMessageHandler> handlerClass;
		
		public final Method handleReflectionMethod;
		
		public final String identification;

		public MessageHandlerReflectionTuple(Method handleReflectionMethod) {
			this.handleReflectionMethod = handleReflectionMethod;
			this.handlerClass = (Class<? extends IMessageHandler> )handleReflectionMethod.getDeclaringClass();
			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
			identification = String.format("Proxy[ forward: %s#%s(%s) ]", handlerClass.getSimpleName(),
					MessageHandlerPool.HANDLER_METHOD_NAME, parameterTypes[0].getSimpleName());
		}

		@Override
		public IMessageHandler getInnerObject() {
			return EJoker.getInstance().getEJokerContext().get(handlerClass);
		}

		@Override
		public Future<AsyncTaskResultBase> handleAsync(IMessage message) {
			try {
				return (Future<AsyncTaskResultBase> )handleReflectionMethod.invoke(getInnerObject(), message);
			} catch (Exception e) {
				RipenFuture<AsyncTaskResultBase> ripenFuture = new RipenFuture<AsyncTaskResultBase>();
				ripenFuture.trySetException(e);
				return ripenFuture;
			}
		}

		@Override
		public String toString() {
			return identification;
		}
	}

}
