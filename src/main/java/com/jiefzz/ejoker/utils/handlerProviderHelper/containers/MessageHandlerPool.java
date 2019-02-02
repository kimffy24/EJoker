package com.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.EJoker;
import com.jiefzz.ejoker.infrastructure.IMessage;
import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.infrastructure.IMessageHandlerProxy;
import com.jiefzz.ejoker.infrastructure.InfrastructureRuntimeException;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.io.IOExceptionOnRuntime;
import com.jiefzz.ejoker.z.common.system.extension.AsyncWrapperException;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.functional.IFunction1;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.task.context.lambdaSupport.QIVoidFunction;

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
				List<MessageHandlerReflectionTuple> handlerInvokerList = getProxyAsyncHandlers(messageType);
				handlerInvokerList.add(new MessageHandlerReflectionTuple(method));
			}
		}
		
	}

	public final static List<MessageHandlerReflectionTuple> getProxyAsyncHandlers(Class<? extends IMessage> messageType) {
		return MapHelper.getOrAdd(handlerMapper, messageType, ArrayList::new);
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

		@Deprecated // 此处是使用原生线程来执行任务。
		@Override
		public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message) {
			/// 使用默认的异步任务执行器，就是创建新线程
			return handleAsync(message, c -> {
				RipenFuture<AsyncTaskResult<Void>> ripenFuture = new RipenFuture<>();
				new Thread(() -> {
						try {
							c.trigger();
							ripenFuture.trySetResult(new AsyncTaskResult<>(AsyncTaskStatus.Success, null, null));
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
		public SystemFutureWrapper<AsyncTaskResult<Void>> handleAsync(IMessage message, IFunction1<SystemFutureWrapper<AsyncTaskResult<Void>>, QIVoidFunction> submitter) {
			return submitter.trigger(() -> {
					try {
						handleReflectionMethod.invoke(getInnerObject(), message);
					} catch (IllegalAccessException|IllegalArgumentException e) {
						throw new AsyncWrapperException(e);
					} catch (InvocationTargetException e) {
						e.printStackTrace();
						throw (Exception )e.getCause();
					}
			});
		}

		@Override
		public String toString() {
			return identification;
		}
	}

}
