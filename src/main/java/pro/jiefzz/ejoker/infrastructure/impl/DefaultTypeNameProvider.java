package pro.jiefzz.ejoker.infrastructure.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.system.enhance.MapUtil;

@EService
public class DefaultTypeNameProvider implements ITypeNameProvider {

	private final static Logger logger = LoggerFactory.getLogger(DefaultTypeNameProvider.class);

	private final Map<String, Class<?>> typeDict = new HashMap<>();
	
	private final Map<Class<?>, String> nameDict = new HashMap<>();
	
	private IDecorator decorator = null;
	
	@Override
	public Class<?> getType(String typeName) {
		return MapUtil.getOrAdd(typeDict, typeName, () -> {
			try {
				return Class.forName(null != decorator ? decorator.preGetType(typeName) : typeName);
			} catch (ClassNotFoundException e) {
				logger.error("Could not find aggregate root type by aggregate root type name [{}].", typeName);
				throw new RuntimeException(e);
			}
		});
	}

	@Override
	public String getTypeName(Class<?> clazz) {
		return MapUtil.getOrAdd(nameDict, clazz, c -> {
			String originTypeName = c.getName();
			return null != decorator ?
					decorator.postGetTypeName(originTypeName) :
						originTypeName;
		});
	}

	@Override
	public void applyAlias(Map<Class<?>, String> dict) {
		Set<Entry<Class<?>, String>> entrySet = dict.entrySet();
		for(Entry<Class<?>, String> entry : entrySet) {
			Class<?> preClazz;
			if(null != (preClazz = this.typeDict.putIfAbsent(entry.getValue(), entry.getKey()))) {
				String msg = String.format(" name:%s -> type:%s conflict with name:%s -> type:%s !!!",
						entry.getValue(),
						entry.getKey().getName(),
						entry.getValue(),
						preClazz.getName());
				logger.error(msg);
				throw new RuntimeException(msg);
			}
			String preName;
			if(null != (preName = this.nameDict.putIfAbsent(entry.getKey(), entry.getValue()))) {
				String msg = String.format(" type:%s -> name:%s conflict with type:%s -> name:%s !!!",
						entry.getKey().getName(),
						entry.getValue(),
						entry.getKey().getName(),
						preName);
				logger.error(msg);
				throw new RuntimeException(msg);
			}
		}
	}

	@Override
	public void useDecorator(IDecorator decorator) {
		if(null != this.decorator) {
			throw new RuntimeException(this.getClass().getName() + "#decorator has been set!!!");
		}
		this.decorator = decorator;
	}

}
