package pro.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessage;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessageHandler;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessageHandlerProxy;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.z.io.IOExceptionOnRuntime;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.RipenFuture;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IFunction1;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.AsyncTaskStatus;

/**
 * 由于message类型可以有多个handler，
 * <br>所以用起来会有点复杂。
 * @author kimffy
 *
 */
public class MessageHandlerPool {
	
	private final static Logger logger = LoggerFactory.getLogger(MessageHandlerPool.class);

	public final static String HANDLER_METHOD_NAME = "handleAsync";

	private final static int PARAMETER_AMOUNT = 1;

	private final static Class<?> PARAMETER_TYPE_SUPER_0 = IMessage.class;

	private final static Map<Class<? extends IMessage>, List<MessageHandlerReflectionTuple>> handlerMapper = new HashMap<>();

	public final static void regist(Class<? extends IMessageHandler> implementationHandlerClazz, IFunction<IEjokerContextDev2> ejokerProvider) {
		
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
				if(PARAMETER_TYPE_SUPER_0.equals(parameterTypes[0]))
					continue;
				if (!PARAMETER_TYPE_SUPER_0.isAssignableFrom(parameterTypes[0]))
					throw new InfrastructureRuntimeException(String.format(
							"%s#%s( %s ) first parameters is not accept!!!", actuallyHandlerName,
							HANDLER_METHOD_NAME, parameterTypes[0].getName()));
				Class<? extends IMessage> messageType = (Class<? extends IMessage> )parameterTypes[0];
				{
					// 约束返回类型。 java无法在编译时约束，那就推到运行时上约束吧
					// 这里就是检查返回类型(带泛型)为 SystemFutureWrapper<AsyncTaskResult<Void>>
					boolean isOK = false;
					Type genericReturnType = method.getGenericReturnType();
					if(genericReturnType instanceof ParameterizedType) {
						ParameterizedType parameterizedType = (ParameterizedType )genericReturnType;
						if(parameterizedType.getRawType().equals(SystemFutureWrapper.class)) {
							Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
							if(null != actualTypeArguments && 1 == actualTypeArguments.length) {
								Type type = actualTypeArguments[0];
								if(type instanceof ParameterizedType) {
									ParameterizedType parameterizedTypeInnerLv1 = (ParameterizedType )type;
									if(parameterizedTypeInnerLv1.getRawType().equals(AsyncTaskResult.class)) {
										Type[] actualTypeArgumentsLv1 = parameterizedTypeInnerLv1.getActualTypeArguments();
										if(null != actualTypeArgumentsLv1 && 1 == actualTypeArgumentsLv1.length) {
											Type typeLv2 = actualTypeArgumentsLv1[0];
											if(Void.class.equals(typeLv2)) {
												isOK = true;
											}
										}
									}
								}
							}
						}
					}
					if(!isOK) {
						String errorDesc = String.format("%s#%s should return SystemFutureWrapper<AsyncTaskResult<Void>> !!!", actuallyHandlerName, HANDLER_METHOD_NAME);
						logger.error(errorDesc);
						throw new RuntimeException(errorDesc);
					}
				}
				if(coverSet.contains(messageType.getName()))
					continue;
				coverSet.add(messageType.getName());
				if (!method.isAccessible())
					method.setAccessible(true);
				List<MessageHandlerReflectionTuple> handlerInvokerList = getProxyAsyncHandlers(messageType);
				handlerInvokerList.add(new MessageHandlerReflectionTuple(method, ejokerProvider));
			}
		}
		
	}

	public final static List<MessageHandlerReflectionTuple> getProxyAsyncHandlers(Class<? extends IMessage> messageType) {
		return MapHelper.getOrAdd(handlerMapper, messageType, LinkedList::new);
	}
	
	public static class MessageHandlerReflectionTuple implements IMessageHandlerProxy {
		
		public final Class<? extends IMessageHandler> handlerClass;
		
		public final Method handleReflectionMethod;
		
		public final String identification;
		
		public final IEjokerContextDev2 ejokerContext;
		
		private IMessageHandler realHandler = null;
		
		public MessageHandlerReflectionTuple(Method handleReflectionMethod, IFunction<IEjokerContextDev2> ejokerProvider) {
			this.handleReflectionMethod = handleReflectionMethod;
			this.handlerClass = (Class<? extends IMessageHandler> )handleReflectionMethod.getDeclaringClass();
			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
			identification = String.format("Proxy[ forward: %s#%s(%s) ]", handlerClass.getSimpleName(),
					MessageHandlerPool.HANDLER_METHOD_NAME, parameterTypes[0].getSimpleName());
			this.ejokerContext = ejokerProvider.trigger();
		}

		@Override
		public IMessageHandler getInnerObject() {
			if(null == realHandler)
				return realHandler = ejokerContext.get(handlerClass);
			return realHandler;
		}

		@Deprecated // 此处是使用原生线程来执行任务。
		@Override
		public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message) {
			/// 使用默认的异步任务执行器，就是创建新线程
			return handleAsync(message, c -> {
				RipenFuture<AsyncTaskResult<Void>> ripenFuture = new RipenFuture<>();
				new Thread(() -> {
						try {
							ripenFuture.trySetResult(c.trigger());
						} catch (Exception ex) {
							logger.debug("Message async handler execute faild!!!", ex);
							ripenFuture.trySetResult(new AsyncTaskResult<>(
									(ex instanceof IOException | ex instanceof IOExceptionOnRuntime ? AsyncTaskStatus.IOException : AsyncTaskStatus.Failed),
									ex.getMessage(),
									null)
								);
						}
					}).start();
				return new SystemFutureWrapper<>(ripenFuture);
			});
		}

		/**
		 * submitter为异步任务执行器的调度封装方法
		 */
		@Override
		public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message, IFunction1<SystemFutureWrapper<AsyncTaskResult<Void>>, IFunction<AsyncTaskResult<Void>>> submitter) {
			return submitter.trigger(() -> {
					try {
						@SuppressWarnings("unchecked")
						SystemFutureWrapper<AsyncTaskResult<Void>> result =
								(SystemFutureWrapper<AsyncTaskResult<Void>> )handleReflectionMethod.invoke(getInnerObject(), message);
						return await(result);
					} catch (IllegalAccessException|IllegalArgumentException e) {
						logger.error("Message handle async faild", e);
						return new AsyncTaskResult<>(AsyncTaskStatus.Failed, e.getMessage(), null);
					} catch (InvocationTargetException e) {
						logger.error("Message handle async faild", (Exception )e.getCause());
						return new AsyncTaskResult<>(AsyncTaskStatus.Failed, ((Exception )e.getCause()).getMessage(), null);
					}
			});
		}

		@Override
		public String toString() {
			return identification;
		}
	}

}
