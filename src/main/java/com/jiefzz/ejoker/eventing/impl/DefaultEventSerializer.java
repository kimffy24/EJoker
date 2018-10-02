package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;

@EService
public class DefaultEventSerializer implements IEventSerializer {

	@Dependence
	private IJSONConverter jsonSerializer;
	
	@Override
	public Map<String, String> serializer(List<IDomainEvent<?>> events) {
		Map<String, String> dict = new LinkedHashMap<String, String>();
		for(IDomainEvent<?> event:events)
			dict.put(event.getClass().getName(), jsonSerializer.convert(event));
		return dict;
	}

	@Override
	public List<IDomainEvent<?>> deserializer(Map<String, String> data) {
		List<IDomainEvent<?>> list = new ArrayList<IDomainEvent<?>>();
		Set<Entry<String,String>> entrySet = data.entrySet();
		for(Entry<String,String> entry:entrySet) {
			Class<?> eventType = getType(entry.getKey());
			Object revert = jsonSerializer.revert(entry.getValue(), eventType);
			list.add((IDomainEvent<?> )revert);
		}
		return list;
	}

//	private String encodeTypeInternal(String data, Class<?> eventClazz) {
//		StringBuilder sb = new StringBuilder();
//		sb.append(data.substring(0, data.lastIndexOf('}')));
//		sb.append(" ,\"__class\": \"");
//		sb.append(eventClazz.getName());
//		sb.append('"');
//		sb.append('}');
//		return sb.toString();
//	}
//	
//	private Class<?> decodeTypeInternal(String ps) {
//		int lastIndexOf = ps.lastIndexOf("__class\": \"");
//		if(0>=lastIndexOf)
//			throw new RuntimeException("Event type lose!!! Original event content: " + ps);
//		String eventTypeString = ps.substring(lastIndexOf, ps.length()-2);
//		return getType(eventTypeString);
//	}
	
	private Class<?> getType(String eventTypeString) {
		Class<?> targetType = cacheTypeMap.get(eventTypeString);
		if(null != targetType)
			return targetType;
		try {
			targetType = Class.forName(eventTypeString);
			cacheTypeMap.put(eventTypeString, targetType);
		} catch (Exception e) {
			throw new RuntimeException("Event type lose!!! Original type string: " + eventTypeString);
		}
		return targetType;
	}
	
	private Map<String, Class<?>> cacheTypeMap = new HashMap<>();
	
}
