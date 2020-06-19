package pro.jk.ejoker.common.utils.genericity;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.jk.ejoker.common.system.enhance.MapUtilx;

public final class GenericExpressionFactory {

	private final static GenericDefinationManagement GDManager = new GenericDefinationManagement();
	
	private final static Map<String, GenericExpression> ExpressionStore = new ConcurrentHashMap<>();
	
	public static GenericExpression getMiddleStatementGenericExpression(Class<?> prototype) {
		
		String expressionSignature = GenericExpression.getExpressionSignature(prototype);
		
		return MapUtilx.getOrAdd(ExpressionStore, expressionSignature, k -> new GenericExpression(GDManager.getOrCreateDefination(prototype)));
		
	}

	public static GenericExpression getGenericExpressDirectly(Class<?> prototype, GenericDefinedType... genericDefinedTypeMetas) {
		
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
			return MapUtilx.getOrAdd(ExpressionStore, parameteriedSignature, s -> new GenericExpression(middleStatementGenericExpression, genericDefinedTypeMetas));
		}
		
	}

	public static GenericExpression getGenericExpress(Type prototype, Type... types) {
		
		// 根据官方文档介绍，java Type类型下有4主要的 类/接口
		//
		//		Type
		//		├── Class
		//		├── ParameterizedType
		//		├── GenericArrayType
		//		└── TypeVariable
		//
		// 后两个暂时不作考虑
		
		if(prototype instanceof Class) {
			
			Class<?> clazz = (Class<?> )prototype;
//			return getGenericExpress(clazz);

			if(null == types || 0 == types.length) {
				GenericExpression middleStatementGenericExpression = getMiddleStatementGenericExpression(clazz);
				if(middleStatementGenericExpression.isComplete())
					return middleStatementGenericExpression;
				else {
					String errInfo = String.format("Unmatch amount of parameterized type!!! target=%s   parameteriedSignature=%s", clazz.getName(), "");
					throw new RuntimeException(errInfo);
				}
			}
			
			GenericDefinedType[] gdtms = new GenericDefinedType[types.length];
			GenericDefination gd = GDManager.getOrCreateDefination(clazz);
			for(int i=0; i<types.length; i++)
				gdtms[i] = new GenericDefinedType(types[i], gd);
			return getGenericExpressDirectly(clazz, gdtms);
			
		} else if(prototype instanceof ParameterizedType) {
			
			ParameterizedType pt = (ParameterizedType )prototype;
			Class<?> clazz = (Class<?> )pt.getRawType();
			types = pt.getActualTypeArguments();
			
			GenericDefinedType[] gdtms = new GenericDefinedType[types.length];
			GenericDefination gd = GDManager.getOrCreateDefination(clazz);
			for(int i=0; i<types.length; i++)
				gdtms[i] = new GenericDefinedType(types[i], gd);
			return getGenericExpressDirectly(clazz, gdtms);
			
		}
		
		return null;
	}

	public static GenericExpression getGenericExpress(TypeRefer<?> typeRef) {
		return getGenericExpress(typeRef.getType());
	}
	
}
