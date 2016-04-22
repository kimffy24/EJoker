package com.jiefzz.ejoker.infrastructure;

public interface IDelegateAction {

	public IDelegateAction setDelegator(Object delegator);
	
	public void delegate();
}
