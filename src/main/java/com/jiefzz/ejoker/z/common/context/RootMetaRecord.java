package com.jiefzz.ejoker.z.common.context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.utilities.ClassNamesScanner;
import com.jiefzz.ejoker.z.common.utilities.GenericTypeUtil;

public class RootMetaRecord {
	
	/**********public field*********/
	
	/**
	 * 记录EService类的所有父类和继承接口。
	 */
	public final Map<Class<?>, Set<Class<?>>> eServiceHierarchyMapper = new HashMap<Class<?>, Set<Class<?>>>();

	/**
	 * 非泛型的 接口类型/抽象类型/实现类型 EService实现类映射对象
	 */
	public final Map<Class<?>, ImplementationTuple> eJokerTheSupportAbstractDirectInstanceTypeMapper = new HashMap<Class<?>, ImplementationTuple>();
	
	/**
	 * 泛型的 接口类型/抽象类型/实现类型 EService实现类映射对象
	 */
	public final Map<Class<?>, GenericityMapper> eJokerTheSupportAbstractGenericityInstanceTypeMapper = new HashMap<Class<?>, GenericityMapper>();

	/**
	 * EService类里面有等待被注入的属性
	 */
	public final Map<Class<?>, Set<Field>> eDependenceMapper = new HashMap<Class<?>, Set<Field>>();

	/**
	 * EInitialize类里面有声明要初始化执行的方法
	 */
	public final Map<Class<?>, Set<Method>> eInitializeMapper = new HashMap<Class<?>, Set<Method>>();
	
	/**********private field*********/

	private final static  Logger logger = LoggerFactory.getLogger(RootMetaRecord.class);
	
	/**
	 * 放入扫描过的包得路径的字符串
	 */
	private final Set<String> hasScanPackage = new HashSet<String>();

	/**
	 * 记录已经经过采集meta信息的类
	 */
	public final Set<Class<?>> hasBeenAnalyzeClass = new HashSet<Class<?>>();
	
	/**********public method*********/
	
	public void annotationScan(String specificPackage) {
		if ( specificPackage.lastIndexOf('.') == (specificPackage.length()-1) )
			specificPackage = specificPackage.substring(0, specificPackage.length()-1);
		for ( String key : hasScanPackage )
			if(specificPackage.startsWith(key)) return;// 传入的包是某个已经被分析的包的子包或就是已存在的包，则不再分析

		List<Class<?>> clazzInSpecificPackage;
		try {
			clazzInSpecificPackage = ClassNamesScanner.scanClass(specificPackage);
			hasScanPackage.add(specificPackage);
		} catch (Exception e) {
			throw new ContextRuntimeException(String.format("Exception occur whild scanning package [%s]!!!", specificPackage), e);
		}

		for (Class<?> clazz:clazzInSpecificPackage) {
			// skip Throwable \ Abstract \ Interface class
			if(Throwable.class.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.getModifiers()) || clazz.isInterface())
				continue;
			analyzeContextAnnotation(clazz);
		}
	}

	/**
	 * DONE: 记录是否已经被分析
	 * DONE: 记录所有@EService
	 * 
	 * DONE: 记录类的@EInitialize方法
	 * DONE: 记录类的@Dependence/@Resource属性
	 * @param clazz
	 */
	public void analyzeContextAnnotation(final Class<?> clazz) {

		// 已经分析过的类就跳过
		if(hasBeenAnalyzeClass.contains(clazz)) return;
		else hasBeenAnalyzeClass.add(clazz);

		// 只关注被标记EService的类
		if(!clazz.isAnnotationPresent(EService.class)) return;

		// 同名方法在在不同的对象中的反射Method是不一样的，用方法名作唯一控制会更好
		// 分析会从子类到父类方向延伸，如果子类中出现过此方法，则会被跳过
		Map<String, Method> reflectMethodStore = new HashMap<>();
		// 同名属性在在不同的对象中的反射Field是不一样的，用属性名作唯一控制会更好
		// 分析会从子类到父类方向延伸，如果子类中出现过此方法，则会被跳过
		Map<String, Field> reflectFieldStore = new HashMap<>();

		// collect the method which annotate by @Initialize .
		Set<Method> annotationMethods = new HashSet<>();
		// collect the properties which annotate by @Dependence or @Resource .
		Set<Field> annotationFields = new HashSet<>();
		// collect the superClass or superInterface
		Set<Class<?>> resolvedHierarchyType = new HashSet<>();

		for ( Class<?> clayy = clazz; clayy != Object.class; clayy = clayy.getSuperclass() ) {

			// 扫描所有被标注为初始化的方法
			Method[] methods = clayy.getDeclaredMethods();
			for ( Method method : methods ) {
				// 已经添加过的方法
				if ( reflectMethodStore.containsKey(method.getName()) ) continue;
				if ( method.isAnnotationPresent(EInitialize.class) ) {

					// TODO 默认规则： 初始化函数不传参
					/// * 带上参数后存在大量分析拆解以及上下文匹配工作，参数完全可以通过其他方法传入
					if(method.getParameterCount()>0) {
						logger.error("Unsupport pass parameters to @Initialize method now!!! Found at {}#{}()", clayy.getName(), method.getName());
						throw new ContextRuntimeException("Unsupport pass parameters to @Initialize method now!!!");
					}
					
					reflectMethodStore.put(method.getName(), method);
					annotationMethods.add(method);
				}
			}

			// 扫描所有标记为依赖的属性
			Field[] fieldArray = clayy.getDeclaredFields();
			for ( Field field : fieldArray ) {
				if ( field.isAnnotationPresent(Dependence.class) || field.isAnnotationPresent(Resource.class) ) {
					Class<?> fieldClazz = field.getType();
					if(fieldClazz.isEnum() || fieldClazz.isArray() || fieldClazz.isPrimitive()) {
						String errInfo = String.format("Unsupport Dependence|Resource annotation on %s.%s", clazz.getName(), field.getName());
						logger.error(errInfo);
						throw new ContextRuntimeException(errInfo);
					}
					if(
							// 当field的声明的类型是泛型时
							GenericTypeUtil.ensureClassIsGenericType(fieldClazz)
							// 且 当泛型变量的泛型签名为空时，抛出错误，并结束操作
							&& GenericTypeUtil.NO_GENERAL_SIGNATURE.equals(GenericTypeUtil.getDeclaredGenericSignature(field))) {
						String errInfo = String.format("Unsupport empty GenericSignature on %s.%s", clazz.getName(), field.getName());
						logger.error(errInfo);
						throw new ContextRuntimeException(errInfo);
					}
					if(!reflectFieldStore.containsKey(field.getName())) { 
						reflectFieldStore.put(field.getName(), field);
						annotationFields.add(field);
					}
				}
			}

			// 收集接口映射 收集父类映射 
			Class<?>[] interfaces = clayy.getInterfaces();
			for ( Class<?> interfaceType : interfaces )
				if(!resolvedHierarchyType.contains(interfaceType))
					resolvedHierarchyType.add(interfaceType);

			// 收集 父类/抽象父类 
			resolvedHierarchyType.add(clayy);
		}
		
		eDependenceMapper.put(clazz, annotationFields);
		eInitializeMapper.put(clazz, annotationMethods);
		eServiceHierarchyMapper.put(clazz, resolvedHierarchyType);
		
		Boolean whetherClazzIsGeneric = GenericTypeUtil.ensureClassIsGenericType(clazz);
		String clazzDefinationGenericSignature = GenericTypeUtil.getClassDefinationGenericSignature(clazz);
		Map<String, String> classSuperGenericSignatureTreex = null;//GenericTypeUtil.getClassSuperGenericSignatureTreex(claxx);
		
		for(Class<?> extractClazz : resolvedHierarchyType) {
			
			if(GenericTypeUtil.ensureClassIsGenericType(extractClazz)) {
				// 父类/接口 是泛型
				GenericityMapper genericityMapper;
				if(null==(genericityMapper = eJokerTheSupportAbstractGenericityInstanceTypeMapper.getOrDefault(extractClazz, null)))
					eJokerTheSupportAbstractGenericityInstanceTypeMapper.put(extractClazz, (genericityMapper = new GenericityMapper()));
				String extractClazzDefinationGenericSignature = GenericTypeUtil.getClassDefinationGenericSignature(extractClazz);
				if(whetherClazzIsGeneric) {
					// 候选实现( 抽象是泛型定义， 实现是泛型定义 )
					if(clazzDefinationGenericSignature.equals(extractClazzDefinationGenericSignature)) {
						// 候选实现需要严格满足泛型签名对称
						if(null == genericityMapper.candidateImplementations)
							genericityMapper.candidateImplementations = new ImplementationTuple().addImplementation(clazz);
						else // 如果候选实现多于1个，输出警告
							warningSameGenericSignature(
									extractClazz,
									extractClazzDefinationGenericSignature,
									genericityMapper.candidateImplementations.addImplementation(clazz)
							);
					} else {
						// 泛型签名不对称，则输出警告
						logger.warn("Could not make {} to be {}'s candidate implementation! Please make sure it is not influence the program!", clazz.getName(), extractClazz.getName());
						logger.warn("Unmatch GenericSignature: \n\t{}\t\t{}\n\t{}\t\t{}",
							extractClazz.getName(), extractClazzDefinationGenericSignature,
							clazz.getName(), clazzDefinationGenericSignature
						);
					}
				} else {
					// 优先的签名实现( 抽象是泛型定义， 实现不是泛型定义 )
					String realGenericSignature = null; //classSuperGenericSignatureTree.get(hierarchyType.getName());
					if(realGenericSignature.equals(extractClazzDefinationGenericSignature)) {
						// 如果 泛型接口的定义上的签名 和 从当前被分析类中分离出来的签名相同，则证明泛型
						//		1. 可能泛型签名不对称
						//		2. 传递丢失
						//			（像 有 interface IBasicA<T>{} ,
						//				又有 interface IBasicB<T> extend IBasicA<T>{}
						//				在 class BImpl implements IBasicB<String> {} 这样的过程中，就会丢失传递。
						logger.warn("{} could not mapping to {}, it may be loss GenericType while in multi inheriting!\n\tPlease make sure it is not influence the program!", extractClazz.getName(), clazz.getName());
						logger.warn("Unmatch GenericSignature: \n\t{}\t\t{}\n\t{}\t\t{}",
								extractClazz.getName(), extractClazzDefinationGenericSignature,
								clazz.getName(), "!!!Lost!!!"
							);
					} else {
						if(genericityMapper.signatureImplementations.containsKey(realGenericSignature)) {
							ImplementationTuple implementationTuple = genericityMapper.signatureImplementations.get(realGenericSignature);
							implementationTuple.addImplementation(clazz);
							warningSameGenericSignature(extractClazz, realGenericSignature, implementationTuple);
						}
						else
							genericityMapper.signatureImplementations.put(realGenericSignature, new ImplementationTuple().addImplementation(clazz));
					}
				}
				
			} else {
				// 父类/接口 非泛型
				if(whetherClazzIsGeneric) {
					// 实现类型是泛型实现( 抽象不是泛型定义， 实现是泛型定义 )，则抽象与实现之间是不可能做到泛型信息对称的，输出警告
					logger.warn("Could not make {} to be {}'s signature implementation! Please make sure it is not influence the program!", clazz.getName(), extractClazz.getName());
					logger.warn("Unmatch Generic Defination: \n\t{}\t\t is not Generic type!\n\t{}\t\t is Generic type with signature {}",
							extractClazz.getName(),
							clazz.getName(), clazzDefinationGenericSignature
						);
				} else {
					// ( 抽象不是泛型定义， 实现不是泛型定义 )
					if(eJokerTheSupportAbstractDirectInstanceTypeMapper.containsKey(extractClazz)) {
						ImplementationTuple implementationTuple = eJokerTheSupportAbstractDirectInstanceTypeMapper.get(extractClazz);
						implementationTuple.addImplementation(clazz);
						warningSameGenericSignature(extractClazz, "", implementationTuple);
					} else
						eJokerTheSupportAbstractDirectInstanceTypeMapper.put(
								extractClazz,
								new ImplementationTuple().addImplementation(clazz)
						);
				}
			}
		}
	}

	/**********private method*********/

	/**
	 * 输出多定义警告
	 * @param hierarchyType
	 * @param genericSignature
	 * @param implementationTuple
	 */
	private void warningSameGenericSignature(Class<?> hierarchyType, String genericSignature, ImplementationTuple implementationTuple) {
		logger.warn("{}{} has more than one implementation!", hierarchyType.getName(), genericSignature);
		int count = implementationTuple.getCountOfImplementations();
		for( int i=0; i<count; i++) {
			logger.warn("\t\t{}{}", implementationTuple.getImplementationsType(i).getName(), genericSignature);
		}
	}
	
	/************ 内部类 *************/
	
	/**
	 * 泛型签名与实现类映射对象
	 * @author jiefzz
	 *
	 */
	public class GenericityMapper {
		
		public ImplementationTuple candidateImplementations = null;
		
		public Map<String, ImplementationTuple> signatureImplementations = new HashMap<String, ImplementationTuple>();
		
	}
	
	/**
	 * 实现类组合
	 * @author jiefzz
	 *
	 */
	public class ImplementationTuple {
		
		private Class<?>[] implementations = new Class<?>[]{};
		
		public ImplementationTuple addImplementation(Class<?> implementedType) {
			Class<?>[] implementations = new Class<?>[this.implementations.length+1];
			System.arraycopy(this.implementations, 0, implementations, 0, this.implementations.length);
			implementations[this.implementations.length] = implementedType;
			this.implementations = implementations;
			return this;
		}
		
		public int getCountOfImplementations() {
			return implementations.length;
		}
		
		public Class<?> getImplementationsType(int index) {
			return implementations[index];
		}
	}
}
