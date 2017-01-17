package com.jiefzz.ejoker.z.common.context.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.IEJokerClassMetaAnalyzer;
import com.jiefzz.ejoker.z.common.context.IEJokerClassMetaProvidor;
import com.jiefzz.ejoker.z.common.context.RootMetaRecord;
import com.jiefzz.ejoker.z.common.context.RootMetaRecord.GenericityMapper;
import com.jiefzz.ejoker.z.common.context.RootMetaRecord.ImplementationTuple;
import com.jiefzz.ejoker.z.common.utilities.GenericTypeUtil;

public class DefaultEJokerClassMetaProvider implements IEJokerClassMetaProvidor,IEJokerClassMetaAnalyzer {
	
	private final static Logger logger = LoggerFactory.getLogger(DefaultEJokerClassMetaProvider.class);
	
	private RootMetaRecord rootMetaRecord = new RootMetaRecord();

	@Override
	public Class<?> resolve(Class<?> type) {
		ImplementationTuple implementationTuple = rootMetaRecord.eJokerTheSupportAbstractDirectInstanceTypeMapper.get(type);
		if(null == implementationTuple || implementationTuple.getCountOfImplementations()==0) {
			throw new ContextRuntimeException("Could not resolve type: " +type.getName());
		}
		int count = implementationTuple.getCountOfImplementations();
		if(count>1) {
			logger.error("{} has more than one implementation!!!", type.getName());
			for(int i=0; i<count; i++)
				logger.error("{} can resolve to {}", type.getName(), implementationTuple.getImplementationsType(i).getName());
			throw new ContextRuntimeException("Ambiguous resolve type: " +type.getName());
		}
		return implementationTuple.getImplementationsType(0);
	}

	@Override
	public Class<?> resolve(Class<?> type, String pSignature) {
		GenericityMapper genericityMapper = rootMetaRecord.eJokerTheSupportAbstractGenericityInstanceTypeMapper.get(type);
		ImplementationTuple implementationTuple;
		if(GenericTypeUtil.NO_GENERAL_SIGNATURE.equals(pSignature)) {
			if(null == (implementationTuple = genericityMapper.candidateImplementations))
				throw new ContextRuntimeException(String.format("Could not resolve type: %s<%s>", type.getName(), pSignature));
		} else {
			if(null == (implementationTuple = genericityMapper.signatureImplementations.getOrDefault(pSignature, null)))
				throw new ContextRuntimeException(String.format("Could not resolve type: %s<%s>", type.getName(), pSignature));
		}

		int count = implementationTuple.getCountOfImplementations();
		if(count>1) {
			logger.error("{}{} has more than one implementation!!!", type.getName(), pSignature);
			for(int i=0; i<count; i++)
				logger.error("{}{} can resolve to {}{}",
						type.getName(), pSignature,
						implementationTuple.getImplementationsType(i).getName(), pSignature
				);
			throw new ContextRuntimeException("Ambiguous resolve type: " +type.getName() +pSignature);
		}
		return implementationTuple.getImplementationsType(0);
	}

	@Override
	public RootMetaRecord getRootMetaRecord() {
		return rootMetaRecord;
	}
	
	@Override
	public void analyzeClassMeta(Class<?> clazz) {
		rootMetaRecord.analyzeContextAnnotation(clazz);
	}

}
