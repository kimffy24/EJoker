package com.jiefzz.ejoker.infrastructure.impl;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;

@EService
public class DefaultTypeNameProvider implements ITypeNameProvider {

	private final static Logger logger = LoggerFactory.getLogger(DefaultTypeNameProvider.class);

	private Map<String, Class<?>> typeDict = new HashMap<>();
	
	@Override
	public Class<?> getType(String typeName) {

		return MapHelper.getOrAdd(typeDict, typeName, () -> {
			try {
				return Class.forName(typeName);
			} catch (ClassNotFoundException e) {
				logger.error("Could not find aggregate root type by aggregate root type name [{}].", typeName);
				throw new RuntimeException(e);
			}
		});
		
	}

}
