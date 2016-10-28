package com.jiefzz.ejoker.z.common.context;

public interface IEJokerClassMetaProvidor {

	public Class<? extends Object> resolve(Class<?> type);

	public Class<? extends Object> resolve(Class<?> type, String pSignature);
	
}
