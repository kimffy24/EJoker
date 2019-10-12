package pro.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.commanding.AbstractCommandHandler;
import pro.jiefzz.ejoker.commanding.CommandRuntimeException;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import pro.jiefzz.ejoker.commanding.ICommandContext;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.z.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.z.system.functional.IFunction;

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
			if(parameterTypes.length==2) {
				if(!ICommandContext.class.isAssignableFrom(parameterTypes[0]) || !ICommand.class.isAssignableFrom(parameterTypes[1]))
					throw new CommandRuntimeException(String.format("%s#%s( %s, %s ) second parameters is not accept!!!", implementationHandlerClass.getName(), method.getName(), parameterTypes[0].getName(), parameterTypes[1].getName()));
				if(!Future.class.isAssignableFrom(method.getReturnType()))
					throw new CommandRuntimeException(String.format("%s#%s( %s, %s ) return type is not accept!!! It should be an implementant of Future", implementationHandlerClass.getName(), method.getName(), parameterTypes[0].getName(), parameterTypes[1].getName()));
				commandType = (Class<? extends ICommand> )parameterTypes[1];
			} else {
				throw new RuntimeException(String.format("Parameter signature of %s#%s is not accept!!!", implementationHandlerClass.getName(), method.getName()));
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
		
		private AbstractCommandHandler asyncHandler = null;
		
		private final IEjokerContextDev2 ejokerContext;

		private AsyncHandlerReflectionMapper(Method asyncHandleReflectionMethod, IFunction<IEjokerContextDev2> ejokerProvider) {
			this.asyncHandleReflectionMethod = asyncHandleReflectionMethod;
			this.asyncHandlerClass = (Class<? extends AbstractCommandHandler> )asyncHandleReflectionMethod.getDeclaringClass();
			Class<?>[] parameterTypes = asyncHandleReflectionMethod.getParameterTypes();
			identification = String.format("Proxy[ forward: %s#%s(%s, %s)]", asyncHandlerClass.getSimpleName(), asyncHandleReflectionMethod.getName(), parameterTypes[0].getSimpleName(), parameterTypes[1].getSimpleName());
			this.ejokerContext = ejokerProvider.trigger();
		}
		
		@Override
		public AbstractCommandHandler getInnerObject() {
			if (null == asyncHandler)
				return asyncHandler = ejokerContext.get(asyncHandlerClass);
			return asyncHandler;
		}
		
		@Override
		public Future<Void> handleAsync(ICommandContext context, ICommand command) {
				try {
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
