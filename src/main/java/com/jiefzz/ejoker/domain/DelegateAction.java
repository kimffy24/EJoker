package com.jiefzz.ejoker.domain;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.infrastructure.impl.JSONConverterUseJsonSmartImpl;
import com.jiefzz.ejoker.utils.DelegateIllegalException;
import com.jiefzz.ejoker.utils.IDelegateAction;

public class DelegateAction<TAggregateRoot extends IAggregateRoot, TDomainEvent extends IDomainEvent> implements IDelegateAction<TAggregateRoot, TDomainEvent> {

	/**
	 * 执行委托
	 * TODO 未完成的委托功能！！！
	 */
	@Override
	public void delegate(TAggregateRoot delegator, TDomainEvent parameter) {
		String eventTypeName = parameter.getClass().getName();
		Method handler;
		try {
			if (eventHandlers.containsKey(eventTypeName))
				handler = eventHandlers.get(eventTypeName);
			else {
				handler = delegator.getClass().getDeclaredMethod("handler", parameter.getClass());
				eventHandlers.put(eventTypeName, handler);
			}
			handler.setAccessible(true);
			handler.invoke(delegator, parameter);
		} catch (Exception e) {
			throw new DelegateIllegalException("class ["+delegator.getClass().getName()+"] haven't declare method [handler] to handler ["+parameter.getClass().getName()+"] !!!", e);
		}
	}
	
	private final Map<String, Method> eventHandlers = new HashMap<String, Method>();

}
