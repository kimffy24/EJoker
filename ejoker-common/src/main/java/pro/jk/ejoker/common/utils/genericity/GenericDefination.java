package pro.jk.ejoker.common.utils.genericity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

import pro.jk.ejoker.common.system.enhance.EachUtilx;
import pro.jk.ejoker.common.system.functional.IVoidFunction1;
import pro.jk.ejoker.common.system.functional.IVoidFunction2;

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
	
	private final GenericDeclaration[] exportGenericDeclares;
	
	private final GenericDefination superDefination;
	
	private final Map<String, String> deliveryMapper;
	
	private final GenericDefinedType[] deliveryTypeMetasTable;
	
	private final Map<Class<?>, GenericDefination> interfaceDefinations;
	
	private final Map<Class<?>, Map<String, String>> interfaceDeliveryMappers;
	
	private final Map<Class<?>, GenericDefinedType[]> interfaceDeliveryTypeMetasTables;
	
	private final Map<String, GenericDefinedField> fieldDefinations;

	public GenericDefination(GenericDefinationManagement gdManager, Class<?> genericPrototype) {
		super();
		this.genericPrototypeClazz = genericPrototype;
		this.isInterface = genericPrototype.isInterface();
		this.definationSignature = genericPrototype.toGenericString();
		
		TypeVariable<?>[] typeParameters = genericPrototype.getTypeParameters();
		if(typeParameters.length != 0) {
			hasGenericDeclare = true;;
			exportGenericDeclares = new GenericDeclaration[typeParameters.length];
			deliveryMapper = new HashMap<>();

			for(int i = 0; i<typeParameters.length; i++) {
				TypeVariable<?> typeVar = typeParameters[i];
				exportGenericDeclares[i] = new GenericDeclaration(this, i, typeVar);
			}
			
		} else {
			hasGenericDeclare = false;
			exportGenericDeclares = null;
			deliveryMapper = null;
		}
		

		if(isInterface || Object.class.equals(genericPrototype) || Object.class.equals(genericPrototype.getSuperclass()))
			superDefination = null;
		else
			superDefination = gdManager.getOrCreateDefination(genericPrototype.getSuperclass());
		
		Class<?>[] interfaces = genericPrototype.getInterfaces();
		if(0 == interfaces.length) {
			interfaceDefinations = null;
			{
				interfaceDeliveryMappers = null;
				interfaceDeliveryTypeMetasTables = null;
			}
		} else {
			interfaceDefinations = new HashMap<>();
			for(Class<?> iface : interfaces) {
				interfaceDefinations.put(iface, gdManager.getOrCreateDefination(iface));
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

			deliveryTypeMetasTable = new GenericDefinedType[actualTypeArguments.length];
			
			if(null != superDefination.exportGenericDeclares && 0 != superDefination.exportGenericDeclares.length) {
				for(int i=0; i<superDefination.exportGenericDeclares.length; i++) {
					GenericDeclaration declareTuple = superDefination.exportGenericDeclares[i];
					Type typeArgument = actualTypeArguments[i];
	
					if(typeArgument instanceof TypeVariable<?>) {
						// generic!
						TypeVariable<?> tv = (TypeVariable<?> )typeArgument;
						deliveryMapper.put("" + i, tv.getTypeName());
						deliveryMapper.put(declareTuple.name, tv.getTypeName());
						deliveryTypeMetasTable[i] = null;
					} else {
						// materialized!
						deliveryTypeMetasTable[i] = new GenericDefinedType(typeArgument, this);
					}
				
				}
			}
			
		} else {
			deliveryTypeMetasTable = null;
		}
		
		Type[] genericInterfaces = genericPrototype.getGenericInterfaces();
		for(Type genericInterface : genericInterfaces) {
			if(genericInterface instanceof ParameterizedType) {
				
				ParameterizedType interfacePt = (ParameterizedType )genericInterface;
				Type[] actualTypeArguments = interfacePt.getActualTypeArguments();

				GenericDefinedType[] ifaceDeliveryTypeMetaTable = new GenericDefinedType[actualTypeArguments.length];
				Map<String, String> ifaceDeliveryMapper = new HashMap<>();
				
				Class<?> iface = (Class<?> )interfacePt.getRawType();
				GenericDeclaration[] eGD = interfaceDefinations.get(iface).exportGenericDeclares;
				if(null != interfaceDefinations.get(iface).exportGenericDeclares
						&& 0 != interfaceDefinations.get(iface).exportGenericDeclares.length)
					for(int i = 0; i<eGD.length; i++) {
						GenericDeclaration declareTuple = eGD[i];
						Type typeArgument = actualTypeArguments[i];
	
						if(typeArgument instanceof TypeVariable<?>) {
							// generic!
							TypeVariable<?> tv = (TypeVariable<?> )typeArgument;
							ifaceDeliveryMapper.put("" + i, tv.getTypeName());
							ifaceDeliveryMapper.put(declareTuple.name, tv.getTypeName());
							ifaceDeliveryTypeMetaTable[i] = null;
						} else {
							// materialized!
							ifaceDeliveryTypeMetaTable[i] = new GenericDefinedType(typeArgument, this);
						}
					}
				
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

	// //// important debug info, please do not delete it.
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
	
	public void forEachGenericDeclares(IVoidFunction1<GenericDeclaration> vf) {
		if(!hasGenericDeclare)
			return;
		if(null == exportGenericDeclares || 0 == exportGenericDeclares.length)
			return;
		for(GenericDeclaration gd : exportGenericDeclares)
			vf.trigger(gd);
	}
	
	public void forEachGenericDeclares(IVoidFunction2<GenericDeclaration, Integer> vf) {
		if(!hasGenericDeclare)
			return;
		if(null == exportGenericDeclares || 0 == exportGenericDeclares.length)
			return;
		for(int i=0; i<exportGenericDeclares.length; i++) {
			vf.trigger(exportGenericDeclares[i], i);
		}
	}
	
	public void forEachInterfaceDefinations(IVoidFunction2<Class<?>, GenericDefination> vf) {
		if(null == interfaceDefinations || interfaceDefinations.isEmpty())
			return;
		EachUtilx.forEach(interfaceDefinations, vf);
	}
	
	public void forEachFieldDefinations(IVoidFunction2<String, GenericDefinedField> vf) {
		if(null == fieldDefinations || fieldDefinations.isEmpty())
			return;
		EachUtilx.forEach(fieldDefinations, vf);
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

	public Map<Class<?>, GenericDefination> getInterfaceDefinations() {
		return interfaceDefinations;
	}

	public GenericDefinedType[] getDeliveryTypeMetasTableCopy() {
		if(null == deliveryTypeMetasTable)
			return null;
		GenericDefinedType[] newOne = new GenericDefinedType[deliveryTypeMetasTable.length];
		System.arraycopy(deliveryTypeMetasTable, 0, newOne, 0, deliveryTypeMetasTable.length);
		return newOne;
	}

	public Map<String, String> getDeliveryMapperCopy() {
		if(null == deliveryMapper || deliveryMapper.isEmpty())
			return null;
		return new HashMap<>(deliveryMapper);
	}
	
	public GenericDefinedType[] getInterfaceDeliveryTypeMetasTableCopy(Class<?> interfaceClazz) {
		if(null == interfaceDeliveryTypeMetasTables)
			return null;
		GenericDefinedType[] interfaceDeliveryTypeMetasTable = interfaceDeliveryTypeMetasTables.get(interfaceClazz);
		if(null == interfaceDeliveryTypeMetasTable)
			return null;

		GenericDefinedType[] newOne = new GenericDefinedType[interfaceDeliveryTypeMetasTable.length];
		System.arraycopy(interfaceDeliveryTypeMetasTable, 0, newOne, 0, interfaceDeliveryTypeMetasTable.length);
		return newOne;
	}

	public Map<String, String> getInterfaceDeliveryMapperCopy(Class<?> interfaceClazz) {
		if(null == interfaceDeliveryMappers || interfaceDeliveryMappers.isEmpty())
			return null;
		Map<String, String> interfaceDeliveryMapper = interfaceDeliveryMappers.get(interfaceClazz);
		if(null == interfaceDeliveryMapper || interfaceDeliveryMapper.isEmpty())
			return null;
		return new HashMap<>(interfaceDeliveryMapper);
	}

	@Override
	public int hashCode() {
		return definationSignature.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericDefination other = (GenericDefination) obj;
		if (definationSignature == null) {
			if (other.definationSignature != null)
				return false;
		} else if (!definationSignature.equals(other.definationSignature))
			return false;
		return true;
	}
	
	
}
