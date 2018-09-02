package com.jiefzz.ejoker.z.common.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.jiefzz.ejoker.z.common.system.functional.IFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

public class GenericExpression {

	/**
	 * 表达的签名
	 */
	public final String expressSignature;
	
	/**
	 * 表达的元泛型数据
	 */
	public final GenericDefination meta;
	
	/**
	 * 导出表<br>
	 * key 为泛型变量的名字
	 */
	protected final Map<String, GenericExpressionExportTuple> exportMapper;
	
	/**
	 * 当前表达的下级表达
	 */
	private final GenericExpression child;

	/**
	 * 当前表达的上级表达
	 */
	private final GenericExpression parent;

	/**
	 * 当前表达的下向实现表达
	 */
	private final GenericExpression implementer;

	/**
	 * 当前表达的上向约束表达
	 */
	private final GenericExpression[] implementsExpressions;
	
	/**
	 * 完全态指示指标
	 */
	private final boolean isComplete;
	
	protected GenericExpression(GenericDefination meta) {
		this(meta, null);
	}
	
	/**
	 * 创建半完全态表达 剔除最终态的类的泛型<br>
	 * 当然，如果最终态类没有泛型，则此表达就是完全态的。
	 * @param meta
	 * @param lowerGenericExpression
	 */
	protected GenericExpression(GenericDefination meta, GenericExpression lowerGenericExpression) {
		this.meta = meta;
		this.expressSignature = getExpressionSignature(meta.getGenericPrototype());
		boolean isInterface = meta.isInterface;
		int genericTypeAmount = meta.getGenericTypeAmount();
		int interfacesAmount = meta.getInterfacesAmount();
		this.implementsExpressions = 0 == interfacesAmount ? null : new GenericExpression[interfacesAmount];
		this.exportMapper = 0 == genericTypeAmount ? null : new HashMap<>();
		
		if(isInterface) {
			// 接口 与 扩展 的关系
			this.child = null;
			this.implementer = lowerGenericExpression;
		} else {
			// 抽象 与 派生 的关系
			this.child = lowerGenericExpression;
			this.implementer = null;
		}
		
		// TODO 获取下级表达式
		GenericExpression lowerExpression = lowerGenericExpression;
		if(null != lowerGenericExpression ) {
			// TODO 获取传递到此的dct
			GenericDefinedTypeMeta[] targetDct = 
					isInterface ? lowerExpression.meta.getInterfaceDeliveryTypeMetasTable(meta.genericPrototype) : lowerExpression.meta.getDeliveryTypeMetasTable();
			// TODO 如果dct中对应的位置存在类型的值，则添加一个导出记录，里面包含 泛型变量名 -> 传递来的类型
			meta.forEachDefinationMeta((tuple, i) -> {
				GenericDefinedTypeMeta currentPassClazz = targetDct[i];
				// 下方传来就是空值，代表这是一个不完全的表达
				if(null == currentPassClazz)
					return;
				exportMapper.put(tuple.name, new GenericExpressionExportTuple(tuple, currentPassClazz));
			});
		}

		final AtomicInteger cursor = new AtomicInteger(0);
		meta.forEachInterfaceDefinationMeta((interfaceClazz, definationMeta) -> {
			// TODO 获取接口的表达
			// 构造函数中会建立接口表达到当前表达的关联
			GenericExpression interfaceGenericExpression = new GenericExpression(definationMeta, GenericExpression.this);
			// add操作会建立从当前表达到接口表达的关联
			int currentIndex = cursor.getAndIncrement();
			if(currentIndex >= getInterfacesAmount())
				throw new RuntimeException();
			GenericExpression.this.implementsExpressions[currentIndex] = interfaceGenericExpression;
		});
		
		GenericDefination superDefinationMeta;
		if(null != (superDefinationMeta  = meta.getSuperDefinationMeta())) {
			this.parent = new GenericExpression(superDefinationMeta, GenericExpression.this);
		} else {
			this.parent = null;
		}

		/**
		 * 不存在泛型 或 导出表数量与泛型数量一致 则为完全形态
		 */
		isComplete = genericTypeAmount == 0  || genericTypeAmount == exportMapper.size() ? true : false;
		
	}
	
	/**
	 * 填入泛型目标 并 自动步进的 复制构造方法
	 * @param target 需要复制的目标
	 * @param lowerGenericExpression 下级表达 （ 或是继承类的表达 或是 接口扩展的表达 ）
	 * @param classes 泛型实例化列表
	 */
	protected GenericExpression(GenericExpression target, GenericExpression lowerGenericExpression, final GenericDefinedTypeMeta... classes) {
		this.meta = target.meta;
		// 在完全态下的复制构造过程中 即便有泛型类型参数表，但也可能在meta中提供了参数表，而不是从参数中传入
		this.expressSignature = getExpressionSignature(target.meta.getGenericPrototype(), classes);
		boolean isInterface = meta.isInterface;
		int genericTypeAmount = meta.getGenericTypeAmount();
		int interfacesAmount = meta.getInterfacesAmount();
		this.exportMapper = 0 == genericTypeAmount ? null : new HashMap<>(target.exportMapper);
		if(!target.isComplete && (null == classes || classes.length != genericTypeAmount))
			throw new RuntimeException();
		
		if(isInterface) {
			this.implementer = lowerGenericExpression;
			this.child = null;
		} else {
			this.implementer = null;
			this.child = lowerGenericExpression;
		}
		
		meta.forEachDefinationMeta((metaTuple, i) -> {
			GenericExpressionExportTuple exportTuple = exportMapper.get(metaTuple.name);
			if(null != exportTuple)
				return;
			GenericDefinedTypeMeta passClazz = classes[i];
			if(null == passClazz)
				throw new RuntimeException();
			exportMapper.put(metaTuple.name, new GenericExpressionExportTuple(metaTuple, passClazz));
		});

		if(0 != interfacesAmount) {
			this.implementsExpressions = new GenericExpression[interfacesAmount];
			for(int i = 0; i<target.implementsExpressions.length; i++) {
				
				GenericExpression upperGe = target.implementsExpressions[i];
				GenericDefinedTypeMeta[] deliveryClassesTable = getDCT(
//						() -> upperGe,
						() -> meta.getInterfaceDeliveryTypeMetasTable(upperGe.meta.genericPrototype),
						() -> meta.getInterfaceDeliveryMapper(upperGe.meta.genericPrototype));
				
				this.implementsExpressions[i] = new GenericExpression(upperGe, this, deliveryClassesTable);
			}
		} else {
			this.implementsExpressions = null;
		}
		
		if(null != target.parent) {
			GenericExpression upperGe = target.parent;
			GenericDefinedTypeMeta[] deliveryClassesTable = getDCT(
//					() -> upperGe,
					() -> meta.getDeliveryTypeMetasTable(),
					() -> meta.getDeliveryMapper());
			this.parent = new GenericExpression(upperGe, this, deliveryClassesTable);
		} else 
			this.parent = null;
		
		isComplete = true;
	}
	
	private GenericDefinedTypeMeta[] getDCT(/*IFunction<GenericExpression> geGetter, */IFunction<GenericDefinedTypeMeta[]> dctGetter, IFunction<Map<String, String>> dmGetter) {
		GenericDefinedTypeMeta[] deliveryTypeMetasTable;
//		GenericExpression ge = geGetter.trigger();
//		if(ge.isComplete) {
//			deliveryClassesTable = new Class<?>[] {};
//		} else {
			deliveryTypeMetasTable = dctGetter.trigger();
			if(null == deliveryTypeMetasTable)
				return deliveryTypeMetasTable;
			Map<String, String> deliveryMapper = dmGetter.trigger();
			for(int j = 0; j<deliveryTypeMetasTable.length; j++) {
				if(null != deliveryTypeMetasTable[j])
					continue;
				String mapperTypeVariableName = deliveryMapper.get("" + j);
				GenericExpressionExportTuple exportTuple = exportMapper.get(mapperTypeVariableName);
				if(null == exportTuple)
					throw new RuntimeException();
				deliveryTypeMetasTable[j] = exportTuple.declarationTypeMeta;
			}
//		}
		return deliveryTypeMetasTable;
	}
	
	public Class<?> getDeclarePrototype() {
		return meta.genericPrototype;
	}

	public int getInterfacesAmount() {
		return meta.getInterfacesAmount();
	}
	
	public boolean isComplete() {
		return isComplete;
	}
	
	public GenericExpression getChild() {
		return child;
	}
	
	public GenericExpression getParent() {
		return parent;
	}
	
	public GenericExpression getImplementer() {
		return implementer;
	}
	
	public void forEachImplementsExpressions(IVoidFunction1<GenericExpression> vf) {
		if(null == this.implementsExpressions || 0 == this.implementsExpressions.length)
			return;
		for(GenericExpression ge : this.implementsExpressions)
			vf.trigger(ge);
	}

	public final static String getExpressionSignature(Class<?> prototype, GenericDefinedTypeMeta... typeMetas) {

		StringBuilder sb = new StringBuilder();
		sb.append(prototype.getName());
		if(null!=typeMetas && 0 != typeMetas.length) {
			sb.append('<');
			for(GenericDefinedTypeMeta typeMeta:typeMetas) {
				sb.append(typeMeta.TypeName);
				sb.append(GenericTypeUtil.SEPARATOR);
			}
			sb.append('>');
		}
		return sb.toString();
		
	}
	
	public final static String getExpressionSignature(Class<?> prototype, Class<?>... classes) {

		StringBuilder sb = new StringBuilder();
		sb.append(prototype.getName());
		if(null!=classes && 0 != classes.length) {
			sb.append('<');
			for(Class<?> clazz:classes) {
				sb.append(clazz.getName());
				sb.append(GenericTypeUtil.SEPARATOR);
			}
			sb.append('>');
		}
		return sb.toString();
		
	}
	
	public final static String getExpressionSignature(Class<?> prototype) {
		return prototype.getName();
	}

}
