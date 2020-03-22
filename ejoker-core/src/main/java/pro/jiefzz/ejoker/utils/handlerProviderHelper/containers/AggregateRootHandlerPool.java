package pro.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.system.enhance.StringUtilx;
import pro.jiefzz.ejoker.common.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.domain.AbstractAggregateRoot;
import pro.jiefzz.ejoker.eventing.IDomainEvent;

public class AggregateRootHandlerPool {
	
	private final static Logger logger = LoggerFactory.getLogger(AggregateRootHandlerPool.class);
	
	private final static Map<Class<? extends IDomainEvent<?>>, HandlerReflectionMapper> handlerMapper = new HashMap<>();
	
	public final static void regist(Class<? extends AbstractAggregateRoot<?>> aggregateRootClass) {
		final Method[] declaredMethods = aggregateRootClass.getDeclaredMethods();
		for( int i=0; i<declaredMethods.length; i++ ) {
			Method method = declaredMethods[i];
			if(!"handle".equals(method.getName()))
				continue;
			if(method.getParameterCount()!=1)
				continue;
			if(!method.isAccessible())
				method.setAccessible(true);
			Class<?>[] parameterTypes = method.getParameterTypes();
			if(null==parameterTypes || 1<parameterTypes.length)
				throw new RuntimeException(String.format("Parameter signature of %s#%s is not accept!!!", aggregateRootClass.getName(), method.getName()));
			if(!IDomainEvent.class.isAssignableFrom(parameterTypes[0]))
				throw new RuntimeException(String.format("%s#%s(%s) parameter type is not accept!!!", aggregateRootClass.getName(), method.getName(), parameterTypes[0].getSimpleName()));
			Class<? extends IDomainEvent<?>> domainEventType = (Class<? extends IDomainEvent<?>> )parameterTypes[0];
			if(null!=handlerMapper.putIfAbsent(domainEventType, new HandlerReflectionMapper(method)))
				throw new RuntimeException(String.format("DomainEvent[type=%s] has more than one handler!!!", domainEventType.getName()));
		}
	}
	
	public final static void invokeInternalHandler(AbstractAggregateRoot<?> agr, IDomainEvent<?> evnt){
		HandlerReflectionMapper hrm = handlerMapper.getOrDefault(evnt.getClass(), null);
		if(null==hrm)
			throw new RuntimeException(String.format("Handler for DomainEvent[type=%s] is not found!!!", evnt.getClass().getName()));
		hrm.handle(agr, evnt);
	}
	
	public final static class HandlerReflectionMapper {
		
		private final Method handleReflectionMethod;
		
		private final String identification;
		
		private HandlerReflectionMapper(Method handleReflectionMethod) {
			this.handleReflectionMethod = handleReflectionMethod;
			Class<?>[] parameterTypes = handleReflectionMethod.getParameterTypes();
			identification = StringUtilx.fmt("Proxy::{}#{}({})]",
					handleReflectionMethod.getDeclaringClass().getName(),
					handleReflectionMethod.getName(),
					parameterTypes[0].getSimpleName());
		}
		
		public void handle(AbstractAggregateRoot<?> aggregateRoot, IDomainEvent<?> domainEvent) {
			try {
				handleReflectionMethod.invoke(aggregateRoot, domainEvent);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new AsyncWrapperException(e);
			} catch (InvocationTargetException e) {
				throw new AsyncWrapperException(e.getCause());
			}
		}
		
		@Override
		public String toString() {
			return identification;
		}
	}
}
