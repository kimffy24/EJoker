package com.jiefzz.ejoker.domain;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.infrastructure.DelegateIllegalException;
import com.jiefzz.ejoker.infrastructure.IDelegateAction;

public class DelegateAction<TAggregateRoot extends IAggregateRoot, TDomainEvent extends IDomainEvent> implements IDelegateAction {

	private TAggregateRoot delegator;
	
	@Override
	public void delegate() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 限定只能设置一次委托对象
	 */
	@SuppressWarnings("unchecked")
	@Override
	public IDelegateAction setDelegator(Object delegator) {
		if (this.delegator!=null) throw new DelegateIllegalException("This delefator is already has an owner!!!");
		TAggregateRoot client;
		try {
			client = (TAggregateRoot ) delegator;
			this.delegator = client;
		} catch ( ClassCastException cce ) {
			throw new DelegateIllegalException("This delefator is not create by " + delegator.getClass().getName(), cce);
		}
		return this;
	}

}
