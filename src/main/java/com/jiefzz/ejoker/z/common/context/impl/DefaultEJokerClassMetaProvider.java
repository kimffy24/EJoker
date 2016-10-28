package com.jiefzz.ejoker.z.common.context.impl;

import com.jiefzz.ejoker.z.common.context.IEJokerClassMetaAnalyzer;
import com.jiefzz.ejoker.z.common.context.IEJokerClassMetaProvidor;

public class DefaultEJokerClassMetaProvider implements IEJokerClassMetaProvidor,IEJokerClassMetaAnalyzer {
	
	private RootMetaRecord rootMetaRecord = new RootMetaRecord();

	@Override
	public Class<? extends Object> resolve(Class<?> type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends Object> resolve(Class<?> type, String pSignature) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void analyzeClassMeta(Class<?> clazz) {
		rootMetaRecord.analyzeContextAnnotation(clazz);
	}

}
