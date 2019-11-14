package pro.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.service.IJSONConverter;
import pro.jiefzz.ejoker.eventing.IDomainEvent;
import pro.jiefzz.ejoker.eventing.IEventSerializer;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;

/**
 * 线性化和立体化协议，此职责应该由用户自己负起，<br>
 * 此处提供一个参考实现
 * @author kimffy
 *
 */
public class DefaultEventSerializer implements IEventSerializer {

	@Dependence
	private IJSONConverter jsonSerializer;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	@Override
	public Map<String, String> serializer(Collection<IDomainEvent<?>> events) {
		Map<String, String> dict = new LinkedHashMap<>();
		for(IDomainEvent<?> event:events)
			dict.put(typeNameProvider.getTypeName(event.getClass()), jsonSerializer.convert(event));
		return dict;
	}

	@Override
	public List<IDomainEvent<?>> deserializer(Map<String, String> data) {
		List<IDomainEvent<?>> list = new ArrayList<IDomainEvent<?>>();
		Set<Entry<String, String>> entrySet = data.entrySet();
		for(Entry<String, String> entry:entrySet) {
			Class<?> eventType = typeNameProvider.getType(entry.getKey());
			Object revert = jsonSerializer.revert(entry.getValue(), eventType);
			list.add((IDomainEvent<?> )revert);
		}
		return list;
	}

}
