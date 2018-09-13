package com.jiefzz.ejoker.z.common.utilities.relationship.extension;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.utilities.relationship.AbstractTypeAnalyze;
import com.jiefzz.ejoker.z.common.utilities.relationship.ParameterizedTypeUtil;
import com.jiefzz.ejoker.z.common.utils.relationship.RelationshipTreeUtilCallbackInterface;
import com.jiefzz.ejoker.z.common.utils.relationship.SpecialTypeCodec;
import com.jiefzz.ejoker.z.common.utils.relationship.SpecialTypeCodecStore;
import com.jiefzz.ejoker.z.common.utils.relationship.UnsupportTypes;

/**
 * 实现无限级的装配工具<br>
 * * 此实例不搭配对应的反序列化工具<br>
 * * 使用FIFO队列实现，并不绝对无限，如果结构打包任务数量超过 Integer.MAX_VALUE，依然汇报异常<br>
 * * TODO 优化实现可以考虑自己实现链表管理了打包任务，这样能使用上无限堆空间。<br>
 * @author JiefzzLon
 *
 * @param <ContainerKVP>
 * @param <ContainerVP>
 */
public class UnlimitedRelationshipTreeUtil<ContainerKVP, ContainerVP> extends AbstractTypeAnalyze {

	private final static Logger logger = LoggerFactory.getLogger(UnlimitedRelationshipTreeUtil.class);

	private RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval = null;

	private SpecialTypeCodecStore<?> specialTypeHandler = null;

	private ThreadLocal<Queue<AbstractTask>> taskQueueBox = ThreadLocal.withInitial(new Supplier<Queue<AbstractTask>>() {
		@Override
		public Queue<AbstractTask> get() {
			return new LinkedBlockingQueue<>();
		}
	});

	public UnlimitedRelationshipTreeUtil(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval) {
		this.eval = eval;
	}

	public UnlimitedRelationshipTreeUtil(RelationshipTreeUtilCallbackInterface<ContainerKVP, ContainerVP> eval,
			SpecialTypeCodecStore<?> specialTypeHandler) {
		this(eval);
		this.specialTypeHandler = specialTypeHandler;
	}

	public ContainerKVP processKVP(Object bean) {
		Queue<AbstractTask> taskQueue = taskQueueBox.get();
		try {
			ContainerKVP resutl = innerAssemblingKVP(bean);
			AbstractTask currentTask;
			while(null != (currentTask = taskQueue.poll())) {
				currentTask.process();
			}
			taskQueueBox.remove();
			return resutl;
		} finally {
			taskQueueBox.remove();
		}
	}

	public ContainerVP processVP(Object bean) {
		Queue<AbstractTask> taskQueue = taskQueueBox.get();
		try {
			ContainerVP resutl = innerAssemblingVP(bean);
			AbstractTask currentTask;
			while(null != (currentTask = taskQueue.poll())) {
				currentTask.process();
			}
			return resutl;
		} finally {
			taskQueueBox.remove();
		}
	}
	
	private void join(AbstractTask task) {
		Queue<AbstractTask> taskQueue = taskQueueBox.get();
		if(!taskQueue.offer(task)) {
			throw new RuntimeException("Task Queue has no more capacity!!!");
		}
	}

	private ContainerKVP processNumalObject(Object bean) {
		if (bean == null)
			return null;

		ContainerKVP keyValueSet = eval.createNode();
		Class<?> clazz = bean.getClass();
		Map<String, Field> analyzeClazzInfo = analyzeClazzInfo(clazz);
		Set<Entry<String, Field>> fieldSet = analyzeClazzInfo.entrySet();
		for (Entry<String, Field> fieldTuple : fieldSet) {
			String fieldName = fieldTuple.getKey();
			Field field = fieldTuple.getValue();
			Class<?> fieldType = field.getType();
			field.setAccessible(true);

			// 先取出field对应的value值，
			// @important@ 从类中取出类型，和从值中取出类型的结果差别比较大
			// @important@ 因为有泛型的存在，尽量从值中取类型来做判断
			Object value;
			try {
				value = field.get(bean);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Could not get [{}] from [{}]!!!", fieldName, clazz.getName());
				throw new RuntimeException("Could not get field value!!!", e);
			}
			if (value == null) {
				continue;
			}
			Class<?> valueType = value.getClass();
			SpecialTypeCodec handler;

			if (ParameterizedTypeUtil.isDirectSerializableType(fieldType)) {
				// 键为基础类型 （类型明确，直接写入）
				eval.addToKeyValueSet(keyValueSet, value, fieldName);
			} else if (fieldType == Object.class && ParameterizedTypeUtil.isDirectSerializableType(valueType)) {
				// 值为基础数据 （逻辑上泛型解析即为类型明确，，直接写入）
				eval.addToKeyValueSet(keyValueSet, value, fieldName);
			} else if (ParameterizedTypeUtil.hasSublevel(fieldType)) {
				// Java集合类型
				if (value instanceof Queue)
					throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
				if (value instanceof Collection || value instanceof Map) {
//					if(strict) {
//						ParameterizedType fieldGenericType = (ParameterizedType )field.getGenericType();
//				        Type[] fieldActualTypeArguments = fieldGenericType.getActualTypeArguments();
//				        if(value instanceof Collection) {
//				        	Type vType = fieldActualTypeArguments[0];
//				        	if(GenericTypeUtil.ensureIsGenericType(vType))
//								throw new RuntimeException(String.format("Unsupport GenericType in %s#%s!!!", clazz.getName(), fieldName));
//				        } else {
//				        	Type kType = fieldActualTypeArguments[0];
//				        	Type vType = fieldActualTypeArguments[1];
//				        	if(!ParameterizedTypeUtil.isDirectSerializableType(kType)) {
//				        		throw new RuntimeException(String.format("Unsupport KeyType in %s#%s!!!", clazz.getName(), fieldName));
//				        	}
//				        	if(GenericTypeUtil.ensureIsGenericType(vType)) {
//				        		throw new RuntimeException(String.format("Unsupport ValueType in %s#%s!!!", clazz.getName(), fieldName));
//				        	}
//				        }
//					}
					join(new AssemblyKVPTask(keyValueSet, value, fieldName));
				} else
					throw new RuntimeException(String.format("Unsupport convert type %s!!!", fieldType.getName()));
			} else if (fieldType.isEnum()) {
				// 枚举类型 ******* （解析成字符串） 原来解析为数字，现在更新为解析出字符串
				eval.addToKeyValueSet(keyValueSet, ((Enum) value).name(), fieldName);
			} else if (fieldType.isArray()) {
				// 数组类型
				join(new AssemblyKVPTask(keyValueSet, value, fieldName));
			} else if (null != specialTypeHandler && null != (handler = specialTypeHandler.getCodec(valueType))) {
				// 存在用户期望使用的解析器,
//				if (!strict || valueType.equals(fieldType)) {
//					// 非严格模式 或者 类型明确的前提下, 则优先使用
					eval.addToKeyValueSet(keyValueSet, handler.encode(value), fieldName);
//				} else {
//					// 否则 （情况包含 严格模式 或 类型不明确对应）
//					throw new RuntimeException(String.format("Unsupport type %s, unexcepted on value of map %s.%s",
//							valueType.getName(), clazz.getName(), fieldName));
//				}
			} else if (UnsupportTypes.isUnsupportType(clazz)) {
				// 明确不支持的类型
				// 如果还是希望使用，可以声明用户自定义解析器
				throw new RuntimeException(String.format("Unsupport type %s, unexcepted on field %s.%s",
						valueType.getName(), clazz, fieldName));
			} else {
				join(new AssemblyKVPTask(keyValueSet, value, fieldName));
			}
		}
		return keyValueSet;
	}

	/**
	 * 装配键值集合的方法
	 * 
	 * @param eval
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private ContainerKVP innerAssemblingKVP(Object object) {
		if (object == null)
			return null;
		if (object instanceof Map) {
			// 工具类里的map
			ContainerKVP resultKVContainer = eval.createNode();
			Map<String, Object> objMap = (Map<String, Object>) object;

			// TODO 严格模式时
			// TODO 此处应该先解析出Map的泛型类型。
			// TODO 当且仅当泛型明确时多类型解析才能成立。

			Set<Entry<String, Object>> entrySet = objMap.entrySet();
			for (Entry<String, Object> entry : entrySet) {
				Object value = entry.getValue();
				if (value == null)
					continue;
				Class<?> valueType = value.getClass();
				String key = entry.getKey();

				SpecialTypeCodec handler;
				if (ParameterizedTypeUtil.isDirectSerializableType(value)) {
					// 基础类型
					eval.addToKeyValueSet(resultKVContainer, value, key);
				} else if (ParameterizedTypeUtil.hasSublevel(value)) {
					// Java集合类型
					if (value instanceof Queue)
						throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
					if (value instanceof Collection || value instanceof Map) {
						join(new AssemblyKVPTask(resultKVContainer, value, key));
					} else
						throw new RuntimeException(String.format("Unsupport convert type %s!!!", valueType.getName()));
				} else if (value.getClass().isEnum()) {
					// 枚举类型
					eval.addToKeyValueSet(resultKVContainer, ((Enum) value).name(), key);
				} else if (value.getClass().isArray()) {
					// 数组类型
					join(new AssemblyKVPTask(resultKVContainer, value, key));
				} else if (null != specialTypeHandler && null != (handler = specialTypeHandler.getCodec(valueType))) {
					// 存在用户期望使用的解析器,
//					if (!strict/* || valueType.equals(fieldType) */) {
//						// 非严格模式 或者 泛型类型明确的前提下, 则优先使用
						eval.addToKeyValueSet(resultKVContainer, handler.encode(value), key);
//					} else {
//						// 否则 （情况包含 严格模式 或 泛型类型不明确对应）
//						throw new RuntimeException(String.format("Unsupport type %s, unexcepted on value of map %s.%s",
//								valueType.getName(), "(**unanalysable**)", key));
//					}
				} else if (UnsupportTypes.isUnsupportType(valueType)) {
					// 明确不支持的类型
					// 如果还是希望使用，可以声明用户自定义解析器
					throw new RuntimeException(String.format("Unsupport type %s, unexcepted on value of map %s.%s",
							valueType.getName(), "(**unanalysable**)", key));
				} else {
					// 普通类类型
					join(new AssemblyKVPTask(resultKVContainer, value, key));
				}
			}
			return resultKVContainer;
		} else {
			// 普通对象
			return processNumalObject(object);
		}
	}

	/**
	 * 装配值集合的方法
	 * 
	 * @param eval
	 * @param object
	 * @return
	 * @throws Exception
	 */
	private ContainerVP innerAssemblingVP(Object object) {
		ContainerVP valueSet = eval.createValueSet();
		if (object.getClass().isArray()) {
			// 数组
			Class<?> clazz = object.getClass().getComponentType();
			if (clazz.isPrimitive())
				privateTypeForEach(valueSet, clazz, object);
			else {
				Object[] objArray = (Object[]) object;
				for (Object value : objArray) {
					// 不需要对空的值集操作
					if (value == null)
						continue;
					innerAssemblingVPSkeleton(valueSet, value);
				}
			}
		} else {
			Collection<?> objCollection = (Collection<?>) object;
			for (Object value : objCollection) {
				// 不需要对空的值集操作
				if (value == null)
					continue;
				innerAssemblingVPSkeleton(valueSet, value);
			}
		}
		return valueSet;
	}

	/**
	 * 分离出skeleton主要是为了避免array类型转换为集合类再执行VP装配操作。
	 * 
	 * @param valueSet
	 * @param value
	 */
	private void innerAssemblingVPSkeleton(ContainerVP valueSet, Object value) {
		Class<?> valueType = value.getClass();
		SpecialTypeCodec handler;
		if (ParameterizedTypeUtil.isDirectSerializableType(value)) {
			// 基础类型
			eval.addToValueSet(valueSet, value);
		} else if (ParameterizedTypeUtil.hasSublevel(value)) {
			// Java集合类型
			if (value instanceof Queue)
				throw new RuntimeException("Unsupport convert type java.util.Queue!!!");
			if (value instanceof Collection || value instanceof Map) {
				join(new AssemblyVPTask(valueSet, value));
			} else
				throw new RuntimeException(String.format("Unsupport convert type %s!!!", valueType.getName()));
		} else if (value.getClass().isEnum()) {
			// 枚举类型
			eval.addToValueSet(valueSet, ((Enum) value).name());
		} else if (value.getClass().isArray()) {
			// 数组类型
			join(new AssemblyVPTask(valueSet, value));
		} else if (null != specialTypeHandler && null != (handler = specialTypeHandler.getCodec(valueType))) {
			// 存在用户期望使用的解析器,
//			if (!strict/* || valueType.equals(fieldType) */) {
//				// 非严格模式 或者 泛型类型明确的前提下, 则优先使用
				eval.addToValueSet(valueSet, handler.encode(value));
//			} else {
//				// 否则 （情况包含 严格模式 或 泛型类型不明确对应）
//				throw new RuntimeException(String.format("Unsupport type %s, unexcepted on value of collection %s.%s",
//						valueType.getName(), "(**unanalysable**)", "(**unanalysable**)"));
//			}
		} else if (UnsupportTypes.isUnsupportType(valueType)) {
			// 明确不支持的类型
			// 如果还是希望使用，可以声明用户自定义解析器
			throw new RuntimeException(String.format("Unsupport type %s, unexcepted on value of map %s.%s",
					valueType.getName(), "(**unanalysable**)", "(**unanalysable**)"));
		} else {
			// 普通类类型
			join(new AssemblyVPTask(valueSet, value));
		}
	}

	/**
	 * 尽量减少装箱操作。
	 * 
	 * @param valueSet
	 * @param componentType
	 * @param object
	 */
	private void privateTypeForEach(ContainerVP valueSet, Class<?> componentType, Object object) {
		// long
		if (long.class == componentType) {
			long[] pArray = ((long[]) object);
			for (long value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// integer
		else if (int.class == componentType) {
			int[] pArray = ((int[]) object);
			for (int value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// short
		else if (short.class == componentType) {
			short[] pArray = ((short[]) object);
			for (short value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// double
		else if (double.class == componentType) {
			double[] pArray = ((double[]) object);
			for (double value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// float
		else if (float.class == componentType) {
			float[] pArray = ((float[]) object);
			for (float value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// char
		else if (char.class == componentType) {
			char[] pArray = ((char[]) object);
			for (char value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// byte
		else if (byte.class == componentType) {
			byte[] pArray = ((byte[]) object);
			for (byte value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		}
		// boolean
		else if (boolean.class == componentType) {
			boolean[] pArray = ((boolean[]) object);
			for (boolean value : pArray)
				innerAssemblingVPSkeleton(valueSet, value);
		} else
			// this should never happen!!!
			throw new RuntimeException();
	}

	private abstract class AbstractTask {
		public abstract boolean isKvp();

		public abstract void process();
	}

	private class AssemblyKVPTask extends AbstractTask {

		private final boolean isKvp;
		public final ContainerKVP workNode;
		public final Object originTarget;
		public final String fieldName;

		public AssemblyKVPTask(ContainerKVP workNode, Object originTarget, String fieldName) {
			this.workNode = workNode;
			this.originTarget = originTarget;
			this.fieldName = fieldName;
			isKvp = true;
		}

		@Override
		public boolean isKvp() {
			return isKvp;
		}

		@Override
		public void process() {
			Object result;
			if (originTarget.getClass().isArray() || originTarget instanceof Collection) {
				result = UnlimitedRelationshipTreeUtil.this.innerAssemblingVP(originTarget);
			} else {
				result = UnlimitedRelationshipTreeUtil.this.innerAssemblingKVP(originTarget);
			}
			UnlimitedRelationshipTreeUtil.this.eval.addToKeyValueSet(workNode, result, fieldName);
		}

	}

	private class AssemblyVPTask extends AbstractTask {

		private final boolean isKvp;
		public final ContainerVP workNode;
		public final Object originTarget;

		public AssemblyVPTask(ContainerVP workNode, Object originTarget) {
			this.workNode = workNode;
			this.originTarget = originTarget;
			isKvp = false;
		}

		@Override
		public boolean isKvp() {
			return isKvp;
		}

		@Override
		public void process() {
			Object result;
			if (originTarget.getClass().isArray() || originTarget instanceof Collection) {
				result = UnlimitedRelationshipTreeUtil.this.innerAssemblingVP(originTarget);
			} else {
				result = UnlimitedRelationshipTreeUtil.this.innerAssemblingKVP(originTarget);
			}
			UnlimitedRelationshipTreeUtil.this.eval.addToValueSet(workNode, result);
		}

	}

}
