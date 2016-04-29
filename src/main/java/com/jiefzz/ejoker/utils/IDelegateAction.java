package com.jiefzz.ejoker.utils;

public interface IDelegateAction<TDelegator, TParameter> {

	public void delegate(TDelegator delegator, TParameter parameter);

}
