package pro.jk.ejoker.common.utils.genericity;

import static java.lang.reflect.Array.newInstance;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.system.functional.IFunction;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;
import pro.jk.ejoker.common.utils.GenericTypeUtil;

/**
 * Represents a full type declared structure. <br /><br />
 * 
 * 用于描述定义的完全类型的数据结构
 * @author kimffy
 *
 */
public class GenericDefinedType extends GenericDefinationEssential {
	
	private final static Logger logger = LoggerFactory.getLogger(GenericDefinedType.class);
	
	/**
	 * 是否完全具现化，具现化即所有泛型变量都被替换为实际类型<br />
	 * 只有还有泛型变量，即未完全具现化
	 */
	public final boolean allHasMaterialized;

	/**
	 * 从类或属性定义中获取到的Type
	 */
	public final Type originTye;

	public final String typeName;

	public final int level;

	public final boolean hasGenericDeclare;

	public final boolean isArray;

	public final Class<?> rawClazz;

	public final GenericDefinedType[] deliveryTypeMetasTable;

	public final boolean isWildcardType;

	public final GenericDefinedType[] boundsUpper;

	public final GenericDefinedType[] boundsLower;
	
	public final GenericDefinedType componentTypeMeta;

	public GenericDefinedType(Type regionTye, GenericDefination referMeta, int level) {
		super(referMeta);
		this.originTye = regionTye;
		this.level = level;
		boolean tmpHasMaterialized = true;
		
		if (Class.class.equals(regionTye.getClass())) {
			// 没有任何泛型的普通类型
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
				componentTypeMeta = new GenericDefinedType(rawClazz, referMeta, level + 1);
				// TODO_ 待处理: java在定义数组的时候会抹掉泛型，那么反序列化时会导致不可预测的错误
				// eg: 定义3个类如下
				// public class A<T> {}
				// public class B extends A<String> {}
				// public class C<U> extends A<U> {}
				// 定义数组 A[] array = new A[3];
				// array[0] = new A<Boolean>();
				// array[1] = new B();
				// array[2] = new C<Void>();
				// 此时，序列化和反序列化都无法正常进行。
			} else {
				// case2: type is a common class
				isArray = false;
				rawClazz = regionClazz;
				componentTypeMeta = null;
			}
			
			// fixed: 无需处理，当if (Class.class.equals(regionTye.getClass())) 满足条件时，
			// 数组的Component声明类型不会跟任何泛型相关，那么存在两种情况
			// 1 Component声明类型的定义本身不包含任何相关泛型，符合预期
			// 2 Component声明类型的定义有泛型但是声明时被用户隐去了，属于不可猜测的用户意图，因此只能给适当的警告
			{
				TypeVariable<?>[] typeParameters = rawClazz.getTypeParameters();
				if(null != typeParameters && 0 < typeParameters.length) {
					logger.warn("Without Generic declare, it maybe produce unpredictable errors! [targetClass: {}]", regionTye.getTypeName());
				}
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
			deliveryTypeMetasTable = new GenericDefinedType[actualTypeArguments.length];
			for(int i = 0; i<actualTypeArguments.length; i++) {
				GenericDefinedType passingTypeMeta = new GenericDefinedType(actualTypeArguments[i], referMeta, level + 1);
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

			componentTypeMeta = new GenericDefinedType(genericComponentType, referMeta, level + 1);
			
			if(genericComponentType instanceof TypeVariable<?>) {
				rawClazz = null;
				hasGenericDeclare = false;
				deliveryTypeMetasTable = new GenericDefinedType[] {componentTypeMeta};
				tmpHasMaterialized = false;
			} else {
			
			{
				// !!! check the component type is WildcardType !!!
				if(componentTypeMeta.isWildcardType) {
					throw new RuntimeException("Unsupport a wildcard type on array declare!!!");
				}
			}
			hasGenericDeclare = componentTypeMeta.hasGenericDeclare;
			rawClazz = componentTypeMeta.rawClazz;
			deliveryTypeMetasTable = new GenericDefinedType[componentTypeMeta.deliveryTypeMetasTable.length];
			System.arraycopy(componentTypeMeta.deliveryTypeMetasTable, 0, deliveryTypeMetasTable, 0, componentTypeMeta.deliveryTypeMetasTable.length);
			for(int i = 0; i<componentTypeMeta.deliveryTypeMetasTable.length; i++) {
				GenericDefinedType passingTypeMeta = componentTypeMeta.deliveryTypeMetasTable[i];
				tmpHasMaterialized &= passingTypeMeta.allHasMaterialized;
			}
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
					boundsUpper = new GenericDefinedType[regionUpperBounds.length];
					for(int i = 0; i<regionUpperBounds.length; i++) {
						GenericDefinedType passingTypeMeta = new GenericDefinedType(regionUpperBounds[i], referMeta, level + 1);
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
					boundsLower = new GenericDefinedType[regionLowerBounds.length];
					for(int i = 0; i<regionLowerBounds.length; i++) {
						GenericDefinedType passingTypeMeta = new GenericDefinedType(regionLowerBounds[i], referMeta, level + 1);
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

		
		typeName = generateTypeName();
	}
	
		private String generateTypeName() {
			String tmpTypeName;
			Type regionTye = this.originTye;
			if (Class.class.equals(regionTye.getClass())) {
				tmpTypeName = regionTye.getTypeName();
//				if(isArray)
//					tmpTypeName += "[]";
			} else if(regionTye instanceof ParameterizedType) {
				StringBuilder sb = new StringBuilder();
//				sb.append(rawClazz.getName());
//				sb.append('<');
				for(GenericDefinedType pt : deliveryTypeMetasTable) {
					sb.append(GenericTypeUtil.SEPARATOR);
					sb.append(pt.typeName);
				}
				sb.append('>');
//				tmpTypeName = sb.toString().replaceFirst(GenericTypeUtil.SEPARATOR+">$", ">");
				tmpTypeName = rawClazz.getName() + "<" + sb.toString().substring(2);
			} else if(regionTye instanceof GenericArrayType) {
				tmpTypeName = componentTypeMeta.typeName + "[]";
			} else if(regionTye instanceof WildcardType) {
				tmpTypeName = regionTye.getTypeName();
				// 上下界只能出现其中一个
				// 上界和和下届只要出现，都只能有1个，因此不用考虑 boundsLower[1] 或 boundsLower[1]，
				// java语法上确定他不可能出现
				if ( null != boundsLower && 0 < boundsLower.length) {
					tmpTypeName += " super " + boundsLower[0].typeName;
				} else if ( null != boundsUpper && 0 < boundsUpper.length) {
					if(!Object.class.getName().contentEquals(boundsUpper[0].typeName))
						tmpTypeName += " extends " + boundsUpper[0].typeName;
				}
			} else if(regionTye instanceof TypeVariable<?>) {
				tmpTypeName = regionTye.getTypeName();
				// 泛型类型表达符， 如果是反射进来的，断然是不会有$这种内部类的标识的；
				// 面对用户可能自定义构造泛型表达式的时候，不应该出现 没完成具现化的中间态表达式
				if(tmpTypeName.indexOf('$')>=0)
					throw new RuntimeException("Do you ensure that this statement will happen???");
			} else {
				throw new RuntimeException("Do you ensure that this statement will happen???");
			}
			return tmpTypeName;
		}
	
	public GenericDefinedType(Type regionTye, GenericDefination referMeta) {
		this(regionTye, referMeta, 0);
	}

	/**
	 * 复制构造一个GenericDefinedTypeMeta
	 * @param targetGenericDefinedTypeMeta
	 */
	public GenericDefinedType(GenericDefinedType targetGenericDefinedTypeMeta, Map<String, GenericExpressionExportTuple> materializedMapper) {
		super(targetGenericDefinedTypeMeta.referDefination);
		originTye = targetGenericDefinedTypeMeta.originTye;
		level = targetGenericDefinedTypeMeta.level;
		hasGenericDeclare = targetGenericDefinedTypeMeta.hasGenericDeclare;
		isArray = targetGenericDefinedTypeMeta.isArray;
		isWildcardType = targetGenericDefinedTypeMeta.isWildcardType;
		final AtomicInteger unmaterializedCounter = new AtomicInteger(0);
		
		if(originTye instanceof TypeVariable<?>)
			unmaterializedCounter.incrementAndGet();

		// TODO 此控制块没得到测试
		if(null != targetGenericDefinedTypeMeta.componentTypeMeta) {
			
			GenericDefinedType originalTypeMeta = targetGenericDefinedTypeMeta.componentTypeMeta;
			GenericDefinedType currentTypeMeta;
			String currentTypeName = originalTypeMeta.typeName;
			GenericExpressionExportTuple genericExpressionExportTuple = materializedMapper.get(currentTypeName);
			if(null == genericExpressionExportTuple) {
				currentTypeMeta = new GenericDefinedType(originalTypeMeta, materializedMapper);
			} else {
				currentTypeMeta = new GenericDefinedType(genericExpressionExportTuple.declarationTypeMeta, materializedMapper);
			}
			componentTypeMeta = currentTypeMeta;
			if(currentTypeMeta.allHasMaterialized) {
				rawClazz = getArrayComponent(currentTypeMeta);
			} else {
				rawClazz = null;
				unmaterializedCounter.incrementAndGet();
			}
		} else {
			componentTypeMeta = null;
			rawClazz = targetGenericDefinedTypeMeta.rawClazz;
		}
		
		if(null != targetGenericDefinedTypeMeta.deliveryTypeMetasTable) {
			deliveryTypeMetasTable = new GenericDefinedType[targetGenericDefinedTypeMeta.deliveryTypeMetasTable.length];
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
			boundsUpper = new GenericDefinedType[targetGenericDefinedTypeMeta.boundsUpper.length];
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
			boundsLower = new GenericDefinedType[targetGenericDefinedTypeMeta.boundsLower.length];
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

//		String tmpTypeName;
//		if(isWildcardType) {
//			/// this statement is satisfy for WildcardType.
//			if(null != boundsLower && null != boundsUpper)
//				throw new RuntimeException("Do you ensure that the upper bounds and lower bounds will be exist in the same time???");
//			
//			{
//				StringBuilder sb = new StringBuilder();
//				sb.append('?');
//				if( null != boundsLower && 0 < boundsLower.length) {
//					sb.append(" super ");
//					sb.append(boundsLower[0].typeName);
//				} else if( null != boundsUpper && 0 < boundsUpper.length) {
//					// eliminate the case  <? extends java.lang.Object>
//					if(!Object.class.getName().equals(boundsUpper[0].typeName)) {
//						sb.append(" extends ");
//						sb.append(boundsUpper[0].typeName);
//					}
//				}
//				tmpTypeName = sb.toString();
//			}
//		} else if(this.isArray) {
//			
//		} else if (null == rawClazz) {
//			/// this statement is satisfy for TypeVariable.
//			tmpTypeName = targetGenericDefinedTypeMeta.typeName;
//		} else {
//			tmpTypeName = GenericExpression.getExpressionSignature(targetGenericDefinedTypeMeta.rawClazz, deliveryTypeMetasTable);
//		}
//		
//		typeName = tmpTypeName + (isArray?"[]":"");
		typeName = generateTypeName();
		allHasMaterialized = 0 == unmaterializedCounter.get() ? true : false;
	}
	
	private void copyTypeMetaAndFillGenericity(
			Map<String, GenericExpressionExportTuple> materializedMapper,
			IFunction<GenericDefinedType[]> deliveryTableGetter,
			IVoidFunction2<Integer, GenericDefinedType> effector) {
		GenericDefinedType[] deliveryTable = deliveryTableGetter.trigger();
		for(int i=0; i<deliveryTable.length; i++ ) {
			GenericDefinedType originalTypeMeta = deliveryTable[i];
			GenericDefinedType currentTypeMeta;
			String currentTypeName = originalTypeMeta.typeName;
			GenericExpressionExportTuple genericExpressionExportTuple = materializedMapper.get(currentTypeName);
			if(null == genericExpressionExportTuple) {
				currentTypeMeta = new GenericDefinedType(originalTypeMeta, materializedMapper);
			} else {
				currentTypeMeta = new GenericDefinedType(genericExpressionExportTuple.declarationTypeMeta, materializedMapper);
			}
			effector.trigger(i, currentTypeMeta);
		}
	}
	
	private static Class<?> getArrayComponent(GenericDefinedType currentTypeMeta) {
		if(currentTypeMeta.isArray) {
			Class<?> childArrayComponentClazz = getArrayComponent(currentTypeMeta.componentTypeMeta);
			Object arrayInstance = newInstance(childArrayComponentClazz, 0);
			return arrayInstance.getClass();
		} else {
			return currentTypeMeta.rawClazz;
		}
	}

	@Override
	public String toString() {
		return typeName;
	}
}