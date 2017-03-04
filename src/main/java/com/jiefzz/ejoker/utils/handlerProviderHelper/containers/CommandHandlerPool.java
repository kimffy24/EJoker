package com.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.EJoker;
import com.jiefzz.ejoker.commanding.AbstractCommandHandler;
import com.jiefzz.ejoker.commanding.CommandExecuteTimeoutException;
import com.jiefzz.ejoker.commanding.CommandRuntimeException;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandContext;
import com.jiefzz.ejoker.commanding.ICommandHandlerProxy;

public class CommandHandlerPool {
	
	public final static Map<Class<? extends ICommand>, HandlerReflectionMapper> handlerMapper =
			new HashMap<Class<? extends ICommand>, HandlerReflectionMapper>();
	
	public final static void regist(Class<? extends AbstractCommandHandler> implementationHandlerClass) {
		final Method[] declaredMethods = implementationHandlerClass.getDeclaredMethods();
		for( int i=0; i<declaredMethods.length; i++ ) {
			Method method = declaredMethods[i];
			if(!"handle".equals(method.getName()))
				continue;
			if(method.getParameterCount()!=2)
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
			if(null!=handlerMapper.putIfAbsent(commandType, new HandlerReflectionMapper(method)))
				throw new RuntimeException(String.format("Command[type=%s] has more than one handler!!!", commandType.getName()));
		}
	}
	
	public static class HandlerReflectionMapper implements ICommandHandlerProxy {
		
		public final Class<? extends AbstractCommandHandler> handlerClass;
		public final Method handleReflectionMethod;
		public final String identification;
		
		private AbstractCommandHandler handler = null;

		/**
		 * EJoker context是严格禁止同事获取对象的
		 * <br>用于在判断handler为空需要从上下中获取Handler的时候排他执行。
		 */
		private final static Lock lock4getHandler = new ReentrantLock();
		
		private HandlerReflectionMapper(Method handleReflectionMethod) {
			this.handleReflectionMethod = handleReflectionMethod;
			this.handlerClass = (Class<? extends AbstractCommandHandler> )handleReflectionMethod.getDeclaringClass();
			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
			identification = String.format("Proxy[ forward: %s#%s(%s, %s)]", handlerClass.getSimpleName(), handleReflectionMethod.getName(), parameterTypes[0].getSimpleName(), parameterTypes[1].getSimpleName());
		}
		
		@Override
		public AbstractCommandHandler getInnerObject() {
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
		public void handle(ICommandContext context, ICommand command) {
			try {
				handleReflectionMethod.invoke(getInnerObject(), context, command);
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
