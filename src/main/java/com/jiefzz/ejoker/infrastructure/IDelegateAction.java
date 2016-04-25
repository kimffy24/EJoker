package com.jiefzz.ejoker.infrastructure;

public interface IDelegateAction<TDelegator, TParameter> {

	public void delegate(TDelegator delegator, TParameter parameter);
	
}
