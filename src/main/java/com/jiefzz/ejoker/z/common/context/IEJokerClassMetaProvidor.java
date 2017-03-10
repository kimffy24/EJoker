package com.jiefzz.ejoker.z.common.context;

public interface IEJokerClassMetaProvidor {

	public Class<?> resolve(Class<?> type);

	public Class<?> resolve(Class<?> type, String pSignature);
	
	public RootMetaRecord getRootMetaRecord();
	
	public void executeEInitialize(Class<?> type, Object instance);
	
}
