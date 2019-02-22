package com.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.commanding.AbstractCommandHandler;
import com.jiefzz.ejoker.commanding.CommandExecuteTimeoutException;
import com.jiefzz.ejoker.commanding.CommandRuntimeException;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandContext;
import com.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerContextDev2;
import com.jiefzz.ejoker.z.common.system.functional.IFunction;

public class CommandHandlerPool {
	
	private final Map<Class<? extends ICommand>, HandlerReflectionMapper> handlerMapper =
			new HashMap<>();
	
	public final void regist(Class<? extends AbstractCommandHandler> implementationHandlerClass, IFunction<IEjokerContextDev2> ejokerProvider) {
		final Method[] declaredMethods = implementationHandlerClass.getDeclaredMethods();
		for( int i=0; i<declaredMethods.length; i++ ) {
			Method method = declaredMethods[i];
			if(!"handle".equals(method.getName()))
				continue;
			if(!method.isAccessible())
				method.setAccessible(true);
			Class<?>[] parameterTypes = method.getParameterTypes();
			if(null==parameterTypes || 2!=parameterTypes.length)
				throw new RuntimeException(String.format("Parameter signature of %s#%s is not accept!!!", implementationHandlerClass.getName(), method.getName()));
			if(!ICommandContext.class.isAssignableFrom(parameterTypes[0]))
				throw new CommandRuntimeException(String.format("%s#%s( %s, %s ) fitst parameters is not accept!!!", implementationHandlerClass.getName(), method.getName(), parameterTypes[0].getName(), parameterTypes[1].getName()));
			if(!ICommand.class.isAssignableFrom(parameterTypes[1]))
				throw new CommandRuntimeException(String.format("%s#%s( %s, %s ) second parameters is not accept!!!", implementationHandlerClass.getName(), method.getName(), parameterTypes[0].getName(), parameterTypes[1].getName()));
			Class<? extends ICommand> commandType = (Class<? extends ICommand> )parameterTypes[1];
			
			if(null!=handlerMapper.putIfAbsent(commandType, new HandlerReflectionMapper(method, ejokerProvider)))
				throw new RuntimeException(String.format("Command[type=%s] has more than one handler!!!", commandType.getName()));
		}
	}
	
	public ICommandHandlerProxy fetchCommandHandler(Class<? extends ICommand> commandType) {
		return handlerMapper.get(commandType);
	}
	
	public static class HandlerReflectionMapper implements ICommandHandlerProxy {
		
		public final Class<? extends AbstractCommandHandler> handlerClass;
		public final Method handleReflectionMethod;
		public final String identification;
		
		private AbstractCommandHandler handler = null;
		
		private IFunction<IEjokerContextDev2> ejokerProvider = null;

		private HandlerReflectionMapper(Method handleReflectionMethod, IFunction<IEjokerContextDev2> ejokerProvider) {
			this.handleReflectionMethod = handleReflectionMethod;
			this.handlerClass = (Class<? extends AbstractCommandHandler> )handleReflectionMethod.getDeclaringClass();
			this.ejokerProvider = ejokerProvider;
			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
			identification = String.format("Proxy[ forward: %s#%s(%s, %s)]", handlerClass.getSimpleName(), handleReflectionMethod.getName(), parameterTypes[0].getSimpleName(), parameterTypes[1].getSimpleName());
		}
		
		@Override
		public AbstractCommandHandler getInnerObject() {
			if (null == handler) {
				handler = ejokerProvider.trigger().get(handlerClass);
				return handler;
			} else
				return handler;
		}
		
		@Override
		public void handle(ICommandContext context, ICommand command) throws Exception {
				try {
					handleReflectionMethod.invoke(getInnerObject(), context, command);
				} catch (IllegalAccessException|IllegalArgumentException e) {
					e.printStackTrace();
					throw new CommandExecuteTimeoutException("Command execute failed!!! " +command.toString(), e);
				} catch (InvocationTargetException e) {
					if(null != e.getCause() && e.getCause() instanceof Exception) {
						e.printStackTrace();
						throw (Exception )e.getCause();
					} else
						throw new CommandExecuteTimeoutException("Command execute failed!!! " +command.toString(), e);
				}
		}
		
		@Override
		public String toString() {
			return identification;
		}
		
	}
}
