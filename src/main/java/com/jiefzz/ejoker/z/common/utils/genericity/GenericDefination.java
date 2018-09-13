package com.jiefzz.ejoker.z.common.utils.genericity;

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
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;

/**
 * A Object contain some useful reflect info.<br>
 * We wanna dissect a class and store some import information, specially the generic info;
 * @author jiefzz
 *
 */
public final class GenericDefination {

	private final String definationSignature;
	
	public final Class<?> genericPrototypeClazz;
	
	public final boolean isInterface;
	
	public final boolean hasGenericDeclare;
	
	private final GenericDeclare[] exportGenericDeclares;
	
	private final GenericDefination superDefination;
	
	private final Map<String, String> deliveryMapper;
	
	private final GenericDefinedTypeMeta[] deliveryTypeMetasTable;
	
	private final Map<Class<?>, GenericDefination> interfaceDefinations;
	
	private final Map<Class<?>, Map<String, String>> interfaceDeliveryMappers;
	
	private final Map<Class<?>, GenericDefinedTypeMeta[]> interfaceDeliveryTypeMetasTables;
	
	private final Map<String, GenericDefinedField> fieldDefinations;

	private GenericDefination(Class<?> genericPrototype) {
		super();
		this.genericPrototypeClazz = genericPrototype;
		this.isInterface = genericPrototype.isInterface();
		this.definationSignature = genericPrototype.toGenericString();
		
		TypeVariable<?>[] typeParameters = genericPrototype.getTypeParameters();
		if(typeParameters.length != 0) {
			hasGenericDeclare = true;;
			exportGenericDeclares = new GenericDeclare[typeParameters.length];
			deliveryMapper = new HashMap<>();

			for(int i = 0; i<typeParameters.length; i++) {
				TypeVariable<?> typeVar = typeParameters[i];
				exportGenericDeclares[i] = new GenericDeclare(this, i, typeVar.getName());
			}
			
		} else {
			hasGenericDeclare = false;
			exportGenericDeclares = null;
			deliveryMapper = null;
		}
		

		if(null == genericPrototype.getSuperclass() || Object.class.equals(genericPrototype.getSuperclass()))
			superDefination = null;
		else
			superDefination = getOrCreateDefination(genericPrototype.getSuperclass());
		
		Class<?>[] interfaces = genericPrototype.getInterfaces();
		if(null == interfaces || 0 == interfaces.length) {
			interfaceDefinations = null;
			{
				interfaceDeliveryMappers = null;
				interfaceDeliveryTypeMetasTables = null;
			}
		} else {
			interfaceDefinations = new HashMap<>();
			for(Class<?> iface : interfaces) {
				interfaceDefinations.put(iface, getOrCreateDefination(iface));
			}
			{
				interfaceDeliveryMappers = new HashMap<>();
				interfaceDeliveryTypeMetasTables = new HashMap<>();
			}
		}
		

		Type genericSuperclass = genericPrototype.getGenericSuperclass();
		if (genericSuperclass instanceof ParameterizedType) {

			ParameterizedType superPt = (ParameterizedType )genericSuperclass;
			Type[] actualTypeArguments = superPt.getActualTypeArguments();

			deliveryTypeMetasTable = new GenericDefinedTypeMeta[actualTypeArguments.length];
			
			ForEachUtil.processForEach(superDefination.exportGenericDeclares, (declareTuple, i) -> {

				Type typeArgument = actualTypeArguments[i];

				if(typeArgument instanceof TypeVariable<?>) {
					// generic!
					TypeVariable<?> tv = (TypeVariable<?> )typeArgument;
					deliveryMapper.put("" + i, tv.getTypeName());
					deliveryMapper.put(declareTuple.name, tv.getTypeName());
					deliveryTypeMetasTable[i] = null;
				} else {
					// materialized!
					deliveryTypeMetasTable[i] = new GenericDefinedTypeMeta(typeArgument, this);
				}
				
			});
			
		} else {
			deliveryTypeMetasTable = null;
		}
		
		Type[] genericInterfaces = genericPrototype.getGenericInterfaces();
		for(Type genericInterface : genericInterfaces) {
			if(genericInterface instanceof ParameterizedType) {
				
				ParameterizedType interfacePt = (ParameterizedType )genericInterface;
				Type[] actualTypeArguments = interfacePt.getActualTypeArguments();

				GenericDefinedTypeMeta[] ifaceDeliveryTypeMetaTable = new GenericDefinedTypeMeta[actualTypeArguments.length];
				Map<String, String> ifaceDeliveryMapper = new HashMap<>();
				
				Class<?> iface = (Class<?> )interfacePt.getRawType();
				
				ForEachUtil.processForEach(interfaceDefinations.get(iface).exportGenericDeclares, (declareTuple, i) -> {
					Type typeArgument = actualTypeArguments[i];

					if(typeArgument instanceof TypeVariable<?>) {
						// generic!
						TypeVariable<?> tv = (TypeVariable<?> )typeArgument;
						ifaceDeliveryMapper.put("" + i, tv.getTypeName());
						ifaceDeliveryMapper.put(declareTuple.name, tv.getTypeName());
						ifaceDeliveryTypeMetaTable[i] = null;
					} else {
						// materialized!
						ifaceDeliveryTypeMetaTable[i] = new GenericDefinedTypeMeta(typeArgument, this);
					}
				});
				
//				TypeVariable<?>[] interfaceTypeParameters = iface.getTypeParameters();
//				for(int i = 0; i<actualTypeArguments.length; i++) {
//					Type varType = actualTypeArguments[i];
//					if(varType instanceof TypeVariable<?>) {
//						// generic!
//						TypeVariable<?> tv = (TypeVariable<?> )varType;
//						ifaceDeliveryMapper.put("" + i, tv.getTypeName());
//						ifaceDeliveryMapper.put(interfaceTypeParameters[i].getTypeName(), tv.getTypeName());
//						ifaceDeliveryTypeMetaTable[i] = null;
//					} else {
//						// materialized!
//						ifaceDeliveryTypeMetaTable[i] = new GenericDefinedTypeMeta(varType, this);
//					}
//				}

				interfaceDeliveryMappers.put(iface, ifaceDeliveryMapper);
				interfaceDeliveryTypeMetasTables.put(iface, ifaceDeliveryTypeMetaTable);
				
			}
		}
		
		if(this.isInterface) {
			fieldDefinations = null;
		} else {
			Field[] declaredFields = this.genericPrototypeClazz.getDeclaredFields();
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
	
	public void forEachGenericDeclares(IVoidFunction1<GenericDeclare> vf) {
		if(!hasGenericDeclare)
			return;
		ForEachUtil.processForEach(exportGenericDeclares, vf);
	}
	
	public void forEachGenericDeclares(IVoidFunction2<GenericDeclare, Integer> vf) {
		if(!hasGenericDeclare)
			return;
		ForEachUtil.processForEach(exportGenericDeclares, vf);
	}
	
	public void forEachInterfaceDefinations(IVoidFunction2<Class<?>, GenericDefination> vf) {
		ForEachUtil.processForEach(interfaceDefinations, vf);
	}
	
	public void forEachFieldDefinations(IVoidFunction2<String, GenericDefinedField> vf) {
		ForEachUtil.processForEach(fieldDefinations, vf);
	}
	
	public int getGenericDeclareAmount() {
		return hasGenericDeclare?exportGenericDeclares.length:0;
	}
	
	public int getInterfacesAmount() {
		return null == interfaceDefinations ? 0 : interfaceDefinations.size();
	}
	
	public String getGenericSignature() {
		return definationSignature;
	}

	public Class<?> getGenericPrototypeClazz() {
		return genericPrototypeClazz;
	}

	public boolean checkHasGenericDeclare() {
		return hasGenericDeclare;
	}

	public GenericDefination getSuperDefination() {
		return superDefination;
	}

	public GenericDefinedTypeMeta[] getDeliveryTypeMetasTableCopy() {
		if(null == deliveryTypeMetasTable)
			return null;
		GenericDefinedTypeMeta[] newOne = new GenericDefinedTypeMeta[deliveryTypeMetasTable.length];
		System.arraycopy(deliveryTypeMetasTable, 0, newOne, 0, deliveryTypeMetasTable.length);
		return newOne;
	}

	public Map<String, String> getDeliveryMapperCopy() {
		if(null == deliveryMapper || deliveryMapper.size() == 0)
			return null;
		return new HashMap<>(deliveryMapper);
	}
	
	public GenericDefinedTypeMeta[] getInterfaceDeliveryTypeMetasTableCopy(Class<?> interfaceClazz) {
		if(null == interfaceDeliveryTypeMetasTables)
			return null;
		GenericDefinedTypeMeta[] interfaceDeliveryTypeMetasTable = interfaceDeliveryTypeMetasTables.get(interfaceClazz);
		if(null == interfaceDeliveryTypeMetasTable)
			return null;

		GenericDefinedTypeMeta[] newOne = new GenericDefinedTypeMeta[interfaceDeliveryTypeMetasTable.length];
		System.arraycopy(interfaceDeliveryTypeMetasTable, 0, newOne, 0, interfaceDeliveryTypeMetasTable.length);
		return newOne;
	}

	public Map<String, String> getInterfaceDeliveryMapperCopy(Class<?> interfaceClazz) {
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
		
		public GenericDefination getGenericDefination() {
			return referDefination;
		}
	}
}
