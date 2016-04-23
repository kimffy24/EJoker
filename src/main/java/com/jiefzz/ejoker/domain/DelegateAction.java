package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.extension.infrastructure.impl.JSONConverterUseJsonSmartImpl;
import com.jiefzz.ejoker.infrastructure.DelegateIllegalException;
import com.jiefzz.ejoker.infrastructure.IDelegateAction;

public class DelegateAction<TAggregateRoot extends IAggregateRoot, TDomainEvent extends IDomainEvent> implements IDelegateAction {

	//private ThreadLocal<TAggregateRoot> delegatorThreadorHolder;
	
	/**
	 * 执行委托
	 * TODO 未完成的委托功能！！！
	 */
	@Override
	public void delegate(Object delegator, Object parameter) {
		TAggregateRoot client = convert(delegator);
		System.out.println((new JSONConverterUseJsonSmartImpl()).convert(parameter));
		throw new DelegateIllegalException("Unimplemented!!!");
	}

	@SuppressWarnings("unchecked")
	private TAggregateRoot convert(Object delegator) {
		TAggregateRoot client;
		try {
			client = (TAggregateRoot ) delegator;
		} catch ( ClassCastException cce ) {
			throw new DelegateIllegalException("This delefator is not create by " + delegator.getClass().getName(), cce);
		}
		return client;
	}

}
