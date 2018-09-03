package com.jiefzz.ejoker.z.common.utilities;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction2;

/**
 * A Object contain some useful reflect info.<br>
 * We wanna dissect a class and store some import information, specially the generic info;
 * @author jiefzz
 *
 */
public final class GenericDefination {

	public final String genericSignature;
	
	public final Class<?> genericPrototype;
	
	public final boolean isInterface;
	
	public final boolean isGenericType;
	
	private final GenericDefinedMeta[] exports;
	
	private final Map<String, String> deliveryMapper;
	
	private final GenericDefinedTypeMeta[] deliveryTypeMetasTable;
	
	private final GenericDefination superDefination;
	
	private final Map<Class<?>, Map<String, String>> interfaceDeliveryMappers = new HashMap<>();
	
	private final Map<Class<?>, GenericDefinedTypeMeta[]> interfaceDeliveryTypeMetasTables = new HashMap<>();
	
	private final Map<Class<?>, GenericDefination> interfaceDefinations = new HashMap<>();
	
	private final Map<String, GenericDefinedField> fieldDefinations;

	private GenericDefination(Class<?> genericPrototype) {
		super();
		this.genericPrototype = genericPrototype;
		this.isInterface = genericPrototype.isInterface();
		this.genericSignature = genericPrototype.toGenericString();
		
		TypeVariable<?>[] typeParameters = genericPrototype.getTypeParameters();
		if(typeParameters.length != 0) {
			isGenericType = true;;
			exports = new GenericDefinedMeta[typeParameters.length];
			deliveryMapper = new HashMap<>();

			for(int i = 0; i<typeParameters.length; i++) {
				TypeVariable<?> typeVar = typeParameters[i];
				exports[i] = new GenericDefinedMeta(this, i, typeVar.getName());
			}
			
		} else {
			isGenericType = false;
			exports = null;
			deliveryMapper = null;
		}
		

		if(null == genericPrototype.getSuperclass() || Object.class.equals(genericPrototype))
			superDefination = null;
		else
			superDefination = getOrCreateDefination(genericPrototype.getSuperclass());
		

		Type genericSuperclass = genericPrototype.getGenericSuperclass();
		if (genericSuperclass instanceof ParameterizedType) {

			ParameterizedType pt = (ParameterizedType )genericSuperclass;
			Type[] actualTypeArguments = pt.getActualTypeArguments();

			deliveryTypeMetasTable = new GenericDefinedTypeMeta[actualTypeArguments.length];
			
			superDefination.forEachDefinationMeta((metaTuple, i) -> {

				Type varType = actualTypeArguments[i];

				if(varType instanceof TypeVariable<?>) {
					// generic!
					TypeVariable<?> tv = (TypeVariable<?> )varType;
					deliveryMapper.put("" + i, tv.getTypeName());
					deliveryMapper.put(metaTuple.name, tv.getTypeName());
					deliveryTypeMetasTable[i] = null;
				} else {
					// materialized!
					deliveryTypeMetasTable[i] = new GenericDefinedTypeMeta(varType, this);
				}
				
			});
			
		} else {
			deliveryTypeMetasTable = null;
		}
		
		Type[] genericInterfaces = genericPrototype.getGenericInterfaces();
		for(Type genericInterface : genericInterfaces) {
			if(genericInterface instanceof ParameterizedType) {
				ParameterizedType ptgi = (ParameterizedType )genericInterface;
				Type[] ptgiActualTypeArguments = ptgi.getActualTypeArguments();
				Class<?> interfaceClazz = (Class<?> )ptgi.getRawType();
				TypeVariable<?>[] interfaceTypeParameters = interfaceClazz.getTypeParameters();
				
				GenericDefinedTypeMeta[] passClazz = new GenericDefinedTypeMeta[ptgiActualTypeArguments.length];
				Map<String, String> passMapper = new HashMap<>();
				for(int i = 0; i<ptgiActualTypeArguments.length; i++) {
					Type varType = ptgiActualTypeArguments[i];
					if(varType instanceof TypeVariable<?>) {
						// generic!
						TypeVariable<?> tv = (TypeVariable<?> )varType;
						passMapper.put("" + i, tv.getTypeName());
						passMapper.put(interfaceTypeParameters[i].getTypeName(), tv.getTypeName());
						passClazz[i] = null;
					} else {
						// materialized!
						passClazz[i] = new GenericDefinedTypeMeta(varType, this);
					}
				}
				interfaceDeliveryMappers.put(interfaceClazz, passMapper);
				interfaceDeliveryTypeMetasTables.put(interfaceClazz, passClazz);
				interfaceDefinations.put(interfaceClazz, getOrCreateDefination(interfaceClazz));
			} else if (Class.class.equals(genericInterface.getClass())) {
				interfaceDefinations.put((Class<?> )genericInterface, getOrCreateDefination((Class<?> )genericInterface));
			} else {
				throw new RuntimeException("Unknow type in genericPrototype.getGenericInterfaces()!!!");
			}
		}
		
		if(this.isInterface) {
			fieldDefinations = null;
		} else {
			Field[] declaredFields = this.genericPrototype.getDeclaredFields();
			if(null == declaredFields) {
				fieldDefinations = null;
			} else {
				fieldDefinations = new HashMap<>();
				for(Field field : declaredFields) {
					if(Modifier.isFinal(field.getModifiers()))
						continue;
					if(Modifier.isStatic(field.getModifiers()))
						continue;
					fieldDefinations.put(field.getName(), new GenericDefinedField(this, field));
				}
			}
		}
	}

	// TODO
	//// important debug info, please do not delete it.
//	private static void parseType(Type[] types, int level) {
//		int accountOfSpace = level * 2;
//		for(Type type : types) {
//			print(type.getClass(), accountOfSpace);
//			accountOfSpace += 2; 
//			if(type instanceof ParameterizedType) {
//				ParameterizedType pt = (ParameterizedType )type;
//				print(pt.getTypeName(), accountOfSpace);
//				print(pt.getRawType().getTypeName(), accountOfSpace);
//				Type[] actualTypeArguments = pt.getActualTypeArguments();
//				parseType(actualTypeArguments, level + 2);
//			} else if(type instanceof WildcardType) {
//				WildcardType wt = (WildcardType )type;
//				print(wt.getTypeName(), accountOfSpace);
//				print("upperBounds: ", accountOfSpace);
//				Type[] upperBounds = wt.getUpperBounds();
//				parseType(upperBounds, level + 2);
//				print("lowerBounds: ", accountOfSpace);
//				Type[] lowerBounds = wt.getLowerBounds();
//				parseType(lowerBounds, level + 2);
//			} else if(type instanceof GenericArrayType) {
//				GenericArrayType gat = (GenericArrayType )type;
//				print(gat.getTypeName(), accountOfSpace);
//				Type genericComponentType = gat.getGenericComponentType();
//				parseType(new Type[] {genericComponentType}, level + 2);
//			} else if (Class.class.equals(type.getClass())) {
//				print(((Class<?> )type).getName(), accountOfSpace);
//			}
//			accountOfSpace -= 2; 
//		}
//	}
//
//	private static void print(Object obj, int sublevel) {
//		for(int i=sublevel; i>0; i--)
//			System.out.print(' ');
//		System.out.println(obj);
//	}
	
	public void forEachDefinationMeta(IVoidFunction1<GenericDefinedMeta> vf) {
		if(!isGenericType)
			return;
		ForEachUtil.processForEach(exports, vf);
	}
	
	public void forEachDefinationMeta(IVoidFunction2<GenericDefinedMeta, Integer> vf) {
		if(!isGenericType)
			return;
		ForEachUtil.processForEach(exports, vf);
	}
	
	public void forEachInterfaceDefinations(IVoidFunction2<Class<?>, GenericDefination> vf) {
		ForEachUtil.processForEach(interfaceDefinations, vf);
	}
	
	public void forEachFieldDefinations(IVoidFunction2<String, GenericDefinedField> vf) {
		ForEachUtil.processForEach(fieldDefinations, vf);
	}
	
	public int getGenericTypeAmount() {
		return isGenericType?exports.length:0;
	}
	
	public int getInterfacesAmount() {
		return null == interfaceDefinations ? 0 : interfaceDefinations.size();
	}
	
	public String getGenericSignature() {
		return genericSignature;
	}

	public Class<?> getGenericPrototype() {
		return genericPrototype;
	}

	public boolean isGenericType() {
		return isGenericType;
	}

	public GenericDefination getSuperDefinationMeta() {
		return superDefination;
	}

	public GenericDefinedTypeMeta[] getDeliveryTypeMetasTable() {
		if(null == deliveryTypeMetasTable)
			return null;
		GenericDefinedTypeMeta[] newOne = new GenericDefinedTypeMeta[deliveryTypeMetasTable.length];
		System.arraycopy(deliveryTypeMetasTable, 0, newOne, 0, deliveryTypeMetasTable.length);
		return newOne;
	}

	public Map<String, String> getDeliveryMapper() {
		if(null == deliveryMapper || deliveryMapper.size() == 0)
			return null;
		return new HashMap<>(deliveryMapper);
	}
	
	public GenericDefinedTypeMeta[] getInterfaceDeliveryTypeMetasTable(Class<?> interfaceClazz) {
		if(null == interfaceDeliveryTypeMetasTables)
			return null;
		GenericDefinedTypeMeta[] interfaceDeliveryTypeMetasTable = interfaceDeliveryTypeMetasTables.get(interfaceClazz);
		if(null == interfaceDeliveryTypeMetasTable)
			return null;

		GenericDefinedTypeMeta[] newOne = new GenericDefinedTypeMeta[interfaceDeliveryTypeMetasTable.length];
		System.arraycopy(interfaceDeliveryTypeMetasTable, 0, newOne, 0, interfaceDeliveryTypeMetasTable.length);
		return newOne;
	}

	public Map<String, String> getInterfaceDeliveryMapper(Class<?> interfaceClazz) {
		if(null == interfaceDeliveryMappers || interfaceDeliveryMappers.size() == 0)
			return null;
		Map<String, String> interfaceDeliveryMapper = interfaceDeliveryMappers.get(interfaceClazz);
		if(null == interfaceDeliveryMapper || interfaceDeliveryMapper.size() == 0)
			return null;
		return new HashMap<>(interfaceDeliveryMapper);
	}
	
	/// ========================== ///
	
	private final static Map<Class<?>, GenericDefination> definationStore= new ConcurrentHashMap<>();
	
	public final static GenericDefination defaultGenericDefination = new GenericDefination(Object.class);
	
	public final static GenericDefination getOrCreateDefination(Class<?> prototype) {
		GenericDefination currentDefination;
		if(defaultGenericDefination.equals(currentDefination = definationStore.getOrDefault(prototype, defaultGenericDefination))) {
			if(Object.class.equals(prototype))
				return defaultGenericDefination;
			definationStore.putIfAbsent(prototype, currentDefination = new GenericDefination(prototype));
		}
		return currentDefination;
	}
	
	static {
		definationStore.put(Object.class, defaultGenericDefination);
	}
	
	public static abstract class GenericDefinationRef {
		
		protected final GenericDefination referDefination;
		
		protected GenericDefinationRef (GenericDefination referDefination) {
			this.referDefination = referDefination;
		}
		
	}
}
