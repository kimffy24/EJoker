package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.Collection;
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
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;

@EService
public class DefaultEventSerializer implements IEventSerializer {

	@Dependence
	private IJSONConverter jsonSerializer;
	
	@Override
	public Map<String, String> serializer(Collection<IDomainEvent<?>> events) {
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

	private Class<?> getType(String eventTypeString) {
		return MapHelper.getOrAdd(CacheTypeMap, eventTypeString, () -> {
			try {
				return Class.forName(eventTypeString);
			} catch (Exception e) {
				throw new RuntimeException("Event type lose!!! Original type string: " + eventTypeString);
			}
		});
	}
	
	private static Map<String, Class<?>> CacheTypeMap = new HashMap<>();
	
}
