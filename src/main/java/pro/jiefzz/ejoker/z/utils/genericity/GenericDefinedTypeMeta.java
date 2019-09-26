package pro.jiefzz.ejoker.z.utils.genericity;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import pro.jiefzz.ejoker.z.system.functional.IFunction;
import pro.jiefzz.ejoker.z.system.functional.IVoidFunction2;
import pro.jiefzz.ejoker.z.utils.GenericTypeUtil;
import pro.jiefzz.ejoker.z.utils.genericity.GenericDefination.GenericDefinationRef;

public class GenericDefinedTypeMeta extends GenericDefinationRef {
	
	public final boolean allHasMaterialized;

	public final Type originTye;

	public final String typeName;

	public final int level;

	public final boolean hasGenericDeclare;

	public final boolean isArray;

	public final Class<?> rawClazz;

	public final GenericDefinedTypeMeta[] deliveryTypeMetasTable;

	public final boolean isWildcardType;

	public final GenericDefinedTypeMeta[] boundsUpper;

	public final GenericDefinedTypeMeta[] boundsLower;
	
	public final GenericDefinedTypeMeta componentTypeMeta;

	public GenericDefinedTypeMeta(Type regionTye, GenericDefination referMeta, int level) {
		super(referMeta);
		this.originTye = regionTye;
		this.typeName = regionTye.getTypeName();
		this.level = level;
		boolean tmpHasMaterialized = true;
		
		if (Class.class.equals(regionTye.getClass())) {
			hasGenericDeclare = false;
			deliveryTypeMetasTable = null;
			isWildcardType = false;
			boundsUpper = null;
			boundsLower = null;
			Class<?> regionClazz = (Class<?> )regionTye;
			if(regionClazz.isArray()) {
				// case1: type is a common array
				isArray = true;
				rawClazz = regionClazz.getComponentType();
				componentTypeMeta = new GenericDefinedTypeMeta(rawClazz, referMeta, level + 1);
			} else {
				// case2: type is a common class
				isArray = false;
				rawClazz = regionClazz;
				componentTypeMeta = null;
			}
		} else if(regionTye instanceof ParameterizedType) {
			hasGenericDeclare = true;
			isArray = false;
			isWildcardType = false;
			boundsUpper = null;
			boundsLower = null;
			componentTypeMeta = null;
			ParameterizedType pt = (ParameterizedType )regionTye;
			rawClazz = (Class<?> )pt.getRawType();
			Type[] actualTypeArguments = pt.getActualTypeArguments();
			deliveryTypeMetasTable = new GenericDefinedTypeMeta[actualTypeArguments.length];
			for(int i = 0; i<actualTypeArguments.length; i++) {
				GenericDefinedTypeMeta passingTypeMeta = new GenericDefinedTypeMeta(actualTypeArguments[i], referMeta, level + 1);
				deliveryTypeMetasTable[i] = passingTypeMeta;
				tmpHasMaterialized &= passingTypeMeta.allHasMaterialized;
			}
		} else if(regionTye instanceof GenericArrayType) {
			isWildcardType = false;
			boundsUpper = null;
			boundsLower = null;
			isArray = true;
			GenericArrayType gat = (GenericArrayType )regionTye;
			Type genericComponentType = gat.getGenericComponentType();
			

			componentTypeMeta = new GenericDefinedTypeMeta(genericComponentType, referMeta, level + 1);
			{
				// !!! check the component type is WildcardType !!!
				if(componentTypeMeta.isWildcardType) {
					throw new RuntimeException("Unsupport a wildcard type on array declare!!!");
				}
			}
			hasGenericDeclare = componentTypeMeta.hasGenericDeclare;
			rawClazz = componentTypeMeta.rawClazz;
			deliveryTypeMetasTable = new GenericDefinedTypeMeta[componentTypeMeta.deliveryTypeMetasTable.length];
			System.arraycopy(componentTypeMeta.deliveryTypeMetasTable, 0, deliveryTypeMetasTable, 0, componentTypeMeta.deliveryTypeMetasTable.length);
			for(int i = 0; i<componentTypeMeta.deliveryTypeMetasTable.length; i++) {
				GenericDefinedTypeMeta passingTypeMeta = componentTypeMeta.deliveryTypeMetasTable[i];
				tmpHasMaterialized &= passingTypeMeta.allHasMaterialized;
			}
		} else if(regionTye instanceof WildcardType) {
			hasGenericDeclare = false;
			isArray = false;
			rawClazz = null;
			deliveryTypeMetasTable = null;
			componentTypeMeta = null;
			isWildcardType = true;
			WildcardType wt = (WildcardType )regionTye;
			{
				Type[] regionUpperBounds = wt.getUpperBounds();
				if(null == regionUpperBounds || 0 == regionUpperBounds.length) {
					boundsUpper = null;
				} else {
					boundsUpper = new GenericDefinedTypeMeta[regionUpperBounds.length];
					for(int i = 0; i<regionUpperBounds.length; i++) {
						GenericDefinedTypeMeta passingTypeMeta = new GenericDefinedTypeMeta(regionUpperBounds[i], referMeta, level + 1);
						boundsUpper[i] = passingTypeMeta;
						tmpHasMaterialized &= passingTypeMeta.allHasMaterialized;
					}
				}
			}
			{
				Type[] regionLowerBounds = wt.getLowerBounds();
				if(null == regionLowerBounds || 0 == regionLowerBounds.length) {
					boundsLower = null;
				} else {
					boundsLower = new GenericDefinedTypeMeta[regionLowerBounds.length];
					for(int i = 0; i<regionLowerBounds.length; i++) {
						GenericDefinedTypeMeta passingTypeMeta = new GenericDefinedTypeMeta(regionLowerBounds[i], referMeta, level + 1);
						boundsLower[i] = passingTypeMeta;
						tmpHasMaterialized &= passingTypeMeta.allHasMaterialized;
					}
				}
			}
		} else if(regionTye instanceof TypeVariable<?>) {
			hasGenericDeclare = false;
			isArray = false;
			rawClazz = null;
			deliveryTypeMetasTable = null;
			isWildcardType = false;
			boundsUpper = null;
			boundsLower = null;
			componentTypeMeta = null;
			tmpHasMaterialized = false;
		} else {
			throw new RuntimeException("Do you ensure that this statement will happen???");
		}
		
		allHasMaterialized = tmpHasMaterialized;
	}
	
	public GenericDefinedTypeMeta(Type regionTye, GenericDefination referMeta) {
		this(regionTye, referMeta, 0);
	}

	/**
	 * 复制构造一个GenericDefinedTypeMeta
	 * @param targetGenericDefinedTypeMeta
	 */
	public GenericDefinedTypeMeta(GenericDefinedTypeMeta targetGenericDefinedTypeMeta, Map<String, GenericExpressionExportTuple> materializedMapper) {
		super(targetGenericDefinedTypeMeta.referDefination);
		originTye = targetGenericDefinedTypeMeta.originTye;
		level = targetGenericDefinedTypeMeta.level;
		hasGenericDeclare = targetGenericDefinedTypeMeta.hasGenericDeclare;
		isArray = targetGenericDefinedTypeMeta.isArray;
		rawClazz = targetGenericDefinedTypeMeta.rawClazz;
		isWildcardType = targetGenericDefinedTypeMeta.isWildcardType;
		final AtomicInteger unmaterializedCounter = new AtomicInteger(0);
		
		// TODO 此控制块没得到测试
		if(null != targetGenericDefinedTypeMeta.componentTypeMeta) {
			
			GenericDefinedTypeMeta originalTypeMeta = targetGenericDefinedTypeMeta.componentTypeMeta;
			GenericDefinedTypeMeta currentTypeMeta;
			String currentTypeName = originalTypeMeta.typeName;
			GenericExpressionExportTuple genericExpressionExportTuple = materializedMapper.get(currentTypeName);
			if(null == genericExpressionExportTuple) {
				currentTypeMeta = new GenericDefinedTypeMeta(originalTypeMeta, materializedMapper);
			} else {
				currentTypeMeta = new GenericDefinedTypeMeta(genericExpressionExportTuple.declarationTypeMeta, materializedMapper);
			}
			componentTypeMeta = currentTypeMeta;
			
		} else {
			componentTypeMeta = null;
		}
		
		if(null != targetGenericDefinedTypeMeta.deliveryTypeMetasTable) {
			deliveryTypeMetasTable = new GenericDefinedTypeMeta[targetGenericDefinedTypeMeta.deliveryTypeMetasTable.length];
			copyTypeMetaAndFillGenericity(
					materializedMapper,
					() -> targetGenericDefinedTypeMeta.deliveryTypeMetasTable,
					(i, newTypeMeta) -> {
						if(!newTypeMeta.allHasMaterialized)
							unmaterializedCounter.incrementAndGet();
						deliveryTypeMetasTable[i] = newTypeMeta;
					}
			);
		} else {
			deliveryTypeMetasTable = null;
		}

		if(null != targetGenericDefinedTypeMeta.boundsUpper) {
			boundsUpper = new GenericDefinedTypeMeta[targetGenericDefinedTypeMeta.boundsUpper.length];
			copyTypeMetaAndFillGenericity(
					materializedMapper,
					() -> targetGenericDefinedTypeMeta.boundsUpper,
					(i, newTypeMeta) -> {
						if(!newTypeMeta.allHasMaterialized)
							unmaterializedCounter.incrementAndGet();
						boundsUpper[i] = newTypeMeta;
					}
			);
		} else {
			boundsUpper = null;
		}

		if(null != targetGenericDefinedTypeMeta.boundsLower) {
			boundsLower = new GenericDefinedTypeMeta[targetGenericDefinedTypeMeta.boundsLower.length];
			copyTypeMetaAndFillGenericity(
					materializedMapper,
					() -> targetGenericDefinedTypeMeta.boundsLower,
					(i, newTypeMeta) -> {
						if(!newTypeMeta.allHasMaterialized)
							unmaterializedCounter.incrementAndGet();
						boundsLower[i] = newTypeMeta;
					}
			);
		} else {
			boundsLower = null;
		}

		String tmpTypeName;
		if(isWildcardType) {
			/// this statement is satisfy for WildcardType.
			if(null != boundsLower && null != boundsUpper)
				throw new RuntimeException("Do you ensure that the upper bounds and lower bounds will be exist in the same time???");
			
			{
				StringBuilder sb = new StringBuilder();
				sb.append('?');
				if( null != boundsUpper ) {
					if(1 == boundsUpper.length && Object.class.getName().equals(boundsUpper[0].typeName)) {
						; // eliminate the case  <? extends java.lang.Object>
					} else {
						sb.append(" extends ");
						for(GenericDefinedTypeMeta upperBound : boundsUpper) {
							sb.append(upperBound.typeName);
							sb.append(GenericTypeUtil.SEPARATOR);
						}
					}
				} else if( null != boundsLower ) {
					sb.append("? super ");
					for(GenericDefinedTypeMeta lowerBound : boundsLower) {
						sb.append(lowerBound.typeName);
						sb.append(GenericTypeUtil.SEPARATOR);
					}
				}
				
				tmpTypeName = sb.toString().replaceFirst(GenericTypeUtil.SEPARATOR + "$", "");
			}
		} else if (null == rawClazz) {
			/// this statement is satisfy for TypeVariable.
			tmpTypeName = targetGenericDefinedTypeMeta.typeName;
		} else {
			tmpTypeName = GenericExpression.getExpressionSignature(targetGenericDefinedTypeMeta.rawClazz, deliveryTypeMetasTable);
		}
		
		typeName = tmpTypeName + (isArray?"[]":"");
		allHasMaterialized = 0 == unmaterializedCounter.get() ? true : false;
	}
	
	private void copyTypeMetaAndFillGenericity(
			Map<String, GenericExpressionExportTuple> materializedMapper,
			IFunction<GenericDefinedTypeMeta[]> deliveryTableGetter,
			IVoidFunction2<Integer, GenericDefinedTypeMeta> effector) {
		GenericDefinedTypeMeta[] deliveryTable = deliveryTableGetter.trigger();
		for(int i=0; i<deliveryTable.length; i++ ) {
			GenericDefinedTypeMeta originalTypeMeta = deliveryTable[i];
			GenericDefinedTypeMeta currentTypeMeta;
			String currentTypeName = originalTypeMeta.typeName;
			GenericExpressionExportTuple genericExpressionExportTuple = materializedMapper.get(currentTypeName);
			if(null == genericExpressionExportTuple) {
				currentTypeMeta = new GenericDefinedTypeMeta(originalTypeMeta, materializedMapper);
			} else {
				currentTypeMeta = new GenericDefinedTypeMeta(genericExpressionExportTuple.declarationTypeMeta, materializedMapper);
			}
			effector.trigger(i, currentTypeMeta);
		}
	}
}