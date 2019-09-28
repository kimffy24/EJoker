package pro.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.commanding.AbstractCommandHandler;
import pro.jiefzz.ejoker.commanding.CommandRuntimeException;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandContext;
import pro.jiefzz.ejoker.commanding.ICommandHandlerProxy;
import pro.jiefzz.ejoker.infrastructure.messaging.varieties.publishableException.IPublishableException;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.z.system.functional.IFunction;

public class CommandHandlerPool {
	
	private final static Logger logger = LoggerFactory.getLogger(CommandHandlerPool.class);
	
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
		
		private final IEjokerContextDev2 ejokerContext;

		private HandlerReflectionMapper(Method handleReflectionMethod, IFunction<IEjokerContextDev2> ejokerProvider) {
			this.handleReflectionMethod = handleReflectionMethod;
			this.handlerClass = (Class<? extends AbstractCommandHandler> )handleReflectionMethod.getDeclaringClass();
			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
			identification = String.format("Proxy[ forward: %s#%s(%s, %s)]", handlerClass.getSimpleName(), handleReflectionMethod.getName(), parameterTypes[0].getSimpleName(), parameterTypes[1].getSimpleName());
			this.ejokerContext = ejokerProvider.trigger();
		}
		
		@Override
		public AbstractCommandHandler getInnerObject() {
			if (null == handler)
				return handler = ejokerContext.get(handlerClass);
			return handler;
		}
		
		@Override
		public void handle(ICommandContext context, ICommand command) throws Exception {
				try {
					handleReflectionMethod.invoke(getInnerObject(), context, command);
				} catch (IllegalAccessException|IllegalArgumentException e) {
					logger.error("Command execute failed!!! ", e);
					throw new RuntimeException("Command execute failed!!! " +command.toString(), e);
				} catch (InvocationTargetException e) {
					if(!IPublishableException.class.isAssignableFrom(((Exception )e.getCause()).getClass())) {
						String eMsg = "Command execute failed!!! " +command.toString();
						logger.error(eMsg, (Exception )e.getCause());
					}
					throw (Exception )e.getCause();
				}
		}
		
		@Override
		public String toString() {
			return identification;
		}
		
	}
}
