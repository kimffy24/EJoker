package pro.jiefzz.ejoker.infrastructure.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.system.helper.MapHelper;
import pro.jiefzz.ejoker.z.system.helper.StringHelper;

@EService
public class DefaultTypeNameProvider implements ITypeNameProvider {

	private final static Logger logger = LoggerFactory.getLogger(DefaultTypeNameProvider.class);

	private final Map<String, Class<?>> typeDict = new HashMap<>();
	
	private final Map<Class<?>, String> aliasDict = new HashMap<>();
	
	private IDecorator decorator = null;
	
	@Override
	public Class<?> getType(String typeName) {

		Class<?> preMapType = typeDict.get(typeName);
		if(null != preMapType)
			return preMapType;
		
		String postTypeName = null != decorator ? decorator.preGetType(typeName) : typeName;
		
		return MapHelper.getOrAdd(typeDict, typeName, () -> {
			try {
				return Class.forName(postTypeName);
			} catch (ClassNotFoundException e) {
				logger.error("Could not find aggregate root type by aggregate root type name [{}].", typeName);
				throw new RuntimeException(e);
			}
		});
		
	}

	@Override
	public String getTypeName(Class<?> clazz) {
		String aliasName = aliasDict.get(clazz);
		if(!StringHelper.isNullOrEmpty(aliasName))
			return aliasName;
		String originTypeName = clazz.getName();
		String finalTypeName = null != decorator ? decorator.postGetTypeName(originTypeName) : originTypeName;
		aliasDict.put(clazz, finalTypeName);
		return finalTypeName;
	}

	@Override
	public void applyDictionary(Map<Class<?>, String> dict) {
		this.aliasDict.clear();
		typeDict.clear();
		this.aliasDict.putAll(dict);
		Set<Entry<Class<?>, String>> entrySet = dict.entrySet();
		for(Entry<Class<?>, String> entry : entrySet) {
			typeDict.put(entry.getValue(), entry.getKey());
		}
	}

	@Override
	public void applyDecorator(IDecorator decorator) {
		this.decorator = decorator;
	}

}
