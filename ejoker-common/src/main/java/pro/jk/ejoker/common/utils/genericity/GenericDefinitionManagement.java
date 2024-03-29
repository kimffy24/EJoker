package pro.jk.ejoker.common.utils.genericity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pro.jk.ejoker.common.system.enhance.MapUtilx;

public class GenericDefinitionManagement {
	
	// manager传入null值逻辑是有问题的，但是实际上Object类型在构建GenericDefination时是不会有接口解析和父类解析的
	// 也就不会用到manager，预期是不会报出NullPointException的
	/**
	 * Represents a default GenericDefination, it is also the GenericDefination of Object.class
	 */
	public final static GenericDefinition DefaultGenericDefination = new GenericDefinition(null, Object.class);

	private final Map<Class<?>, GenericDefinition> definationStore= new ConcurrentHashMap<>();
	
	public final GenericDefinition getOrCreateDefination(Class<?> prototype) {
		return MapUtilx.getOrAdd(definationStore, prototype, k -> {
			return (Object.class.equals(prototype))
					? DefaultGenericDefination
							: new GenericDefinition(GenericDefinitionManagement.this, prototype);
		});
//		GenericDefination currentDefination;
//		while(defaultGenericDefination.equals(currentDefination = definationStore.getOrDefault(prototype, defaultGenericDefination))) {
//			if(Object.class.equals(prototype))
//				return defaultGenericDefination;
//			definationStore.putIfAbsent(prototype, new GenericDefination(this, prototype));
//		}
//		return currentDefination;
	}
	
	public GenericDefinitionManagement() {
		definationStore.put(Object.class, DefaultGenericDefination);
	}
	
	
}
