package pro.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.Suspendable;
import pro.jiefzz.ejoker.commanding.AbstractCommandHandler;
import pro.jiefzz.ejoker.commanding.CommandRuntimeException;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandContext;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import pro.jiefzz.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.common.system.enhance.StringUtilx;
import pro.jiefzz.ejoker.common.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jiefzz.ejoker.common.system.functional.IFunction;

public class CommandHandlerPool {
	
	private final static Logger logger = LoggerFactory.getLogger(CommandHandlerPool.class);
	
	private final Map<Class<? extends ICommand>, AsyncHandlerReflectionMapper> asyncHandlerMapper =
			new HashMap<>();
	
	public final void regist(Class<? extends AbstractCommandHandler> implementationHandlerClass, IFunction<IEjokerContextDev2> ejokerProvider) {
		final Method[] declaredMethods = implementationHandlerClass.getDeclaredMethods();
		for( int i=0; i<declaredMethods.length; i++ ) {
			Method method = declaredMethods[i];
			if(!"handleAsync".equals(method.getName()))
				continue;
			if(!method.isAccessible())
				method.setAccessible(true);
			Class<?>[] parameterTypes = method.getParameterTypes();
			if(null==parameterTypes)
				throw new RuntimeException(String.format("Parameter signature of %s#%s is not accept!!!", implementationHandlerClass.getName(), method.getName()));
			Class<? extends ICommand> commandType;
			if(parameterTypes.length!=2) {
				throw new RuntimeException(String.format("Parameter signature of %s#%s is not accept!!!", implementationHandlerClass.getName(), method.getName()));
			}
			
			if(!ICommandContext.class.isAssignableFrom(parameterTypes[0]) || !ICommand.class.isAssignableFrom(parameterTypes[1]))
				throw new CommandRuntimeException(String.format("%s#%s( %s, %s ) second parameters is not accept!!!", implementationHandlerClass.getName(), method.getName(), parameterTypes[0].getSimpleName(), parameterTypes[1].getSimpleName()));
			commandType = (Class<? extends ICommand> )parameterTypes[1];
			
			{
				// 约束返回类型。 java无法在编译时约束，那就推到运行时上约束吧
				// 这里就是检查返回类型(带泛型)为 Future<Void>
				boolean isOK = false;
				Type genericReturnType = method.getGenericReturnType();
				if(genericReturnType instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType )genericReturnType;
					if(parameterizedType.getRawType().equals(Future.class)) {
						Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
						if(null != actualTypeArguments && 1 == actualTypeArguments.length) {
							if(Void.class.equals(actualTypeArguments[0])) {
								isOK = true;
							}
						}
					}
				} else if(void.class.equals(genericReturnType))
					isOK = true;
				
				if(!isOK) {
					String errorDesc = StringUtilx.fill("The method which Proxy will point to should return Future<Void> or declare return void!!! [currentMethod: {}#{}({}, {})]", implementationHandlerClass.getName(), "handleAsync", parameterTypes[0].getSimpleName(), parameterTypes[1].getSimpleName());
					logger.error(errorDesc);
					throw new RuntimeException(errorDesc);
				}
			}
			
			if(null!=asyncHandlerMapper.putIfAbsent(commandType, new AsyncHandlerReflectionMapper(method, ejokerProvider)))
				throw new RuntimeException(String.format("Command[type=%s] has more than one handler!!!", commandType.getName()));
		}
	}
	
	public ICommandHandlerProxy fetchCommandHandler(Class<? extends ICommand> commandType) {
		return asyncHandlerMapper.get(commandType);
	}
	
	public static class AsyncHandlerReflectionMapper implements ICommandHandlerProxy {
		
		public final Class<? extends AbstractCommandHandler> asyncHandlerClass;
		public final Method asyncHandleReflectionMethod;
		public final String identification;
		public final boolean voidReturn;
		
		private AbstractCommandHandler asyncHandler = null;
		
		private final IEjokerContextDev2 ejokerContext;

		private AsyncHandlerReflectionMapper(Method asyncHandleReflectionMethod, IFunction<IEjokerContextDev2> ejokerProvider) {
			this.asyncHandleReflectionMethod = asyncHandleReflectionMethod;
			this.asyncHandlerClass = (Class<? extends AbstractCommandHandler> )asyncHandleReflectionMethod.getDeclaringClass();
			Class<?>[] parameterTypes = asyncHandleReflectionMethod.getParameterTypes();
			identification = StringUtilx.fill("Proxy::{}#{}({}, {})",
					asyncHandlerClass.getSimpleName(),
					asyncHandleReflectionMethod.getName(),
					parameterTypes[0].getSimpleName(),
					parameterTypes[1].getSimpleName());
			this.ejokerContext = ejokerProvider.trigger();
			

			Type genericReturnType = asyncHandleReflectionMethod.getGenericReturnType();
			voidReturn = void.class.equals(genericReturnType) ? true : false;
			
		}
		
		@Override
		public AbstractCommandHandler getInnerObject() {
			if (null == asyncHandler)
				return asyncHandler = ejokerContext.get(asyncHandlerClass);
			return asyncHandler;
		}
		
		@Suspendable
		@Override
		public Future<Void> handleAsync(ICommandContext context, ICommand command) {
				try {
					if(voidReturn) {
						asyncHandleReflectionMethod.invoke(getInnerObject(), context, command);
						return EJokerFutureUtil.completeFuture();
					}
					return (Future<Void> )asyncHandleReflectionMethod.invoke(getInnerObject(), context, command);
				} catch (IllegalAccessException|IllegalArgumentException e) {
//					e.printStackTrace();
					throw new RuntimeException("Command execute failed!!! " +command.toString(), e);
				} catch (InvocationTargetException e) {
//					String eMsg = "Command execute failed!!! " +command.toString();
//					logger.error(eMsg, (Exception )e.getCause());
					throw new AsyncWrapperException(e.getCause());
				}
		}
		
		@Override
		public String toString() {
			return identification;
		}
		
	}
}
