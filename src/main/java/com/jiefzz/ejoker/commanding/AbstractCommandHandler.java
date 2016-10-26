package com.jiefzz.ejoker.commanding;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java could not multi-implement ICommandHandler.<br>
 * 使用直接重载。<br>
 * 同时在此处分析Command对应的CommandHandler<br>
 * @author jiefzz
 *
 */
public abstract class AbstractCommandHandler implements ICommandHandler<ICommand> {
	
	private final static  Logger logger = LoggerFactory.getLogger(AbstractCommandHandler.class);
	
	@Override
	public void handle(ICommandContext context, ICommand command) {
		throw new CommandRuntimeException("Do you forget to implement the handler function to handle command which type is " +command.getClass().getName());
	}
	
	public final static Map<Class<? extends ICommand>, HandlerReflectionMapper> HandlerMapper =
			new HashMap<Class<? extends ICommand>, HandlerReflectionMapper>();
	
	public final static void regist(AbstractCommandHandler implementationHandler) {
		Class<? extends AbstractCommandHandler> implementationHandlerClass = implementationHandler.getClass();
		final Method[] declaredMethods = implementationHandlerClass.getDeclaredMethods();
		for( int i=0; i<declaredMethods.length; i++ ) {
			Method method = declaredMethods[i];
			if(!"handle".equals(method.getName())) continue;
			if(method.getParameterCount()!=2) continue;
			if(!method.isAccessible()) method.setAccessible(true);
			Class<?>[] parameterTypes = method.getParameterTypes();
			if(!ICommandContext.class.isAssignableFrom(parameterTypes[0]))
				throw new CommandRuntimeException(String.format("%s#%s( %s, %s) fitst parameters is not accept!!!", implementationHandler.getClass().getName(), method.getName(), parameterTypes[0].getName(), parameterTypes[1].getName()));
			if(!ICommand.class.isAssignableFrom(parameterTypes[1]))
				throw new CommandRuntimeException(String.format("%s#%s( %s, %s) second parameters is not accept!!!", implementationHandler.getClass().getName(), method.getName(), parameterTypes[0].getName(), parameterTypes[1].getName()));
			Class<? extends ICommand> commandType = (Class<? extends ICommand>) parameterTypes[1];
			HandlerMapper.put(commandType, new HandlerReflectionMapper(implementationHandler, method));
		}
	}
	
	public static class HandlerReflectionMapper implements ICommandHandlerProxy {
		public final AbstractCommandHandler handler;
		public final Method handleReflectionMethod;
		public final String identification;
		private HandlerReflectionMapper(AbstractCommandHandler handler, Method handleReflectionMethod) {
			this.handleReflectionMethod = handleReflectionMethod;
			this.handler = handler;
			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
			identification = String.format("%s#%s( %s, %s)", handler.getClass().getName(), handleReflectionMethod.getName(), parameterTypes[0].getName(), parameterTypes[1].getName());
		}
		
		@Override
		public AbstractCommandHandler getInnerObject() {
			return handler;
		}
		@Override
		public void hadler(ICommandContext context, ICommand command) {
			try {
				handleReflectionMethod.invoke(handler, context, command);
			} catch (Exception e) {
				e.printStackTrace();
				throw new CommandExecuteTimeoutException("Command execute failed!!! " +command.toString(), e);
			}
		}
		@Override
		public String toString() {
			return identification;
		}
	}
}
