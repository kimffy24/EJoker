package com.jiefzz.ejoker.z.common.utils.genericity;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GenericExpressionFactory {

	private final static Map<String, GenericExpression> expressionStore = new ConcurrentHashMap<>();
	
	public final static GenericExpression defaultExpression = new GenericExpression(GenericDefination.getOrCreateDefination(Object.class));

	public final static GenericExpression getGenericExpress(Class<?> prototype, Type... types) {
		
		if(null == types || 0 == types.length)
			throw new RuntimeException();
		
		GenericDefinedTypeMeta[] gdtms = new GenericDefinedTypeMeta[types.length];
		for(int i=0; i<types.length; i++)
			gdtms[i] = new GenericDefinedTypeMeta(types[i], null/*GenericDefination.defaultGenericDefination*/);
		return getGenericExpress(prototype, gdtms);
		
	}

	public final static GenericExpression getGenericExpress(Class<?> prototype, GenericDefinedTypeMeta... genericDefinedTypeMetas) {
		
		if(null == genericDefinedTypeMetas || 0 == genericDefinedTypeMetas.length)
			return getGenericExpress(prototype);
		
		GenericExpression middleStatementGenericExpression = getMiddleStatementGenericExpression(prototype);
		String parameteriedSignature = GenericExpression.getExpressionSignature(prototype, genericDefinedTypeMetas);
		if(null == genericDefinedTypeMetas || 0 == genericDefinedTypeMetas.length) {
			if(middleStatementGenericExpression.isComplete())
				return middleStatementGenericExpression;
			else {
				String errInfo = String.format("Unmatch amount of parameterized type!!! target=%s   parameteriedSignature=%s", prototype.getSimpleName(), parameteriedSignature);
				throw new RuntimeException(errInfo);
			}
		} else {
			GenericExpression fullStatementGenericExpressp;
			if(defaultExpression.equals(fullStatementGenericExpressp = expressionStore.getOrDefault(parameteriedSignature, defaultExpression))) {
				expressionStore.putIfAbsent(parameteriedSignature, fullStatementGenericExpressp = new GenericExpression(middleStatementGenericExpression, null, genericDefinedTypeMetas));
			}
			return fullStatementGenericExpressp;
		}
		
	}
	
	public final static GenericExpression getGenericExpress(Class<?> prototype) {
		GenericExpression middleStatementGenericExpression = getMiddleStatementGenericExpression(prototype);
		if(middleStatementGenericExpression.isComplete())
			return middleStatementGenericExpression;
		else {
			String errInfo = String.format("Unmatch amount of parameterized type!!! target=%s   parameteriedSignature=%s", prototype.getName(), "");
			throw new RuntimeException(errInfo);
		}
	}

	public final static GenericExpression getMiddleStatementGenericExpression(Class<?> prototype) {
		
		String expressionSignature = GenericExpression.getExpressionSignature(prototype);
		
		GenericExpression genericExpression;
		if(defaultExpression.equals(genericExpression = expressionStore.getOrDefault(expressionSignature, defaultExpression))) {
			expressionStore.putIfAbsent(expressionSignature, genericExpression = new GenericExpression(GenericDefination.getOrCreateDefination(prototype)));
		}
		
		return genericExpression;
	}
}
