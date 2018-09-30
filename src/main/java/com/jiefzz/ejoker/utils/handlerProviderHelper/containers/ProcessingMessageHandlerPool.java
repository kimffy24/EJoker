//package com.jiefzz.ejoker.utils.handlerProviderHelper.containers;
//
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Future;
//
//import com.jiefzz.ejoker.EJoker;
//import com.jiefzz.ejoker.infrastructure.IProcessingMessage;
//import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
//import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
//import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
//import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
//import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
//import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
//
///**
// * @author kimffy
// *
// */
//public class ProcessingMessageHandlerPool {
//
//	public final static String HANDLER_METHOD_NAME = "handleAsync";
//
//	private final static int PARAMETER_AMOUNT = 1;
//
//	private final static Class<?> PARAMETER_TYPE_SUPER_0 = IProcessingMessage.class;
//
//	private final static Map<Class<? extends IProcessingMessage<?, ?>>, List<MessageHandlerReflectionTuple>> handlerMapper = new ConcurrentHashMap<>();
//
//	public final static void regist(Class<? extends IProcessingMessageHandler<?, ?>> implementationHandlerClazz) {
//		
//		Set<String> coverSet = new HashSet<>();
//		String actuallyHandlerName = implementationHandlerClazz.getName();
//		for(Class<?> clazz = implementationHandlerClazz; !Object.class.equals(clazz) && null != clazz; clazz = clazz.getSuperclass() ) {
//			final Method[] declaredMethods = clazz.getDeclaredMethods();
//			for (int i = 0; i < declaredMethods.length; i++) {
//				Method method = declaredMethods[i];
//				if (!HANDLER_METHOD_NAME.equals(method.getName()))
//					continue;
//				if (method.getParameterCount() != PARAMETER_AMOUNT)
//					throw new RuntimeException(String.format("Parameter signature of %s#%s is not accept!!!", actuallyHandlerName, method.getName()));
//				Class<?>[] parameterTypes = method.getParameterTypes();
//				if (!PARAMETER_TYPE_SUPER_0.isAssignableFrom(parameterTypes[0]))
//					throw new InfrastructureRuntimeException(String.format(
//							"%s#%s( %s ) first parameters is not accept!!!", actuallyHandlerName,
//							HANDLER_METHOD_NAME, parameterTypes[0].getName()));
//				Class<? extends IProcessingMessage<?, ?>> messageType = (Class<? extends IProcessingMessage<?, ?>> )parameterTypes[0];
//				if(coverSet.contains(messageType.getName()))
//					continue;
//				coverSet.add(messageType.getName());
//				if (!method.isAccessible())
//					method.setAccessible(true);
//				List<MessageHandlerReflectionTuple> handlerInvokerList = MapHelper.getOrAdd(handlerMapper, messageType, () -> new ArrayList<>());
//				handlerInvokerList.add(new MessageHandlerReflectionTuple(method));
//			}
//		}
//		
//	}
//
//	public final static List<MessageHandlerReflectionTuple> getProxyAsyncHandlers(Class<? extends IProcessingMessage<?, ?>> messageType) {
//		return MapHelper.getOrAdd(handlerMapper, messageType, () -> new ArrayList<>());
//	}
//	
//	public static class MessageHandlerReflectionTuple {
//		
//		public final Class<? extends IProcessingMessageHandler<?, ?>> handlerClass;
//		
//		public final Method handleReflectionMethod;
//		
//		public final String identification;
//		
//		private Object handlerInstance = null;
//
//		public MessageHandlerReflectionTuple(Method handleReflectionMethod) {
//			this.handleReflectionMethod = handleReflectionMethod;
//			this.handlerClass = (Class<? extends IProcessingMessageHandler<?, ?>> )handleReflectionMethod.getDeclaringClass();
//			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
//			identification = String.format("Proxy[ forward: %s#%s(%s) ]", handlerClass.getSimpleName(),
//					ProcessingMessageHandlerPool.HANDLER_METHOD_NAME, parameterTypes[0].getSimpleName());
//		}
//
//		public Future<AsyncTaskResultBase> handleAsync(IProcessingMessage<?, ?> processingMessage) {
//			if( null == handlerInstance ) {
//				handlerInstance = EJoker.getInstance().getEJokerContext().get(handlerClass, processingMessage.getClass(), processingMessage.getMessage().getClass());
//				if( null == handlerInstance )
//					throw new ContextRuntimeException(identification + " is not found!!!");
//			}
//			try {
//				return (Future<AsyncTaskResultBase> )handleReflectionMethod.invoke(handlerInstance, processingMessage);
//			} catch (Exception e) {
//				RipenFuture<AsyncTaskResultBase> ripenFuture = new RipenFuture<AsyncTaskResultBase>();
//				ripenFuture.trySetException(e);
//				return ripenFuture;
//			}
//		}
//
//		@Override
//		public String toString() {
//			return identification;
//		}
//	}
//
//}
