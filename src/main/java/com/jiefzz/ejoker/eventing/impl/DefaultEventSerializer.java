package com.jiefzz.ejoker.eventing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultEventSerializer implements IEventSerializer {

	@Dependence
	private IJSONConverter jsonSerializer;
	
	@Override
	public Map<String, String> serializer(Collection<IDomainEvent<?>> events) {
		Map<String, String> dict = new HashMap<String, String>();
		for(IDomainEvent<?> event:events)
			dict.put(event.getClass().getName(), jsonSerializer.convert(event));
		return dict;
	}

	@Override
	public Collection<IDomainEvent<?>> deserializer(Map<String, String> data) {
		List<IDomainEvent<?>> list = new ArrayList<IDomainEvent<?>>();
		Set<Entry<String,String>> entrySet = data.entrySet();
		for(Entry<String,String> entry:entrySet) {
			Class<?> eventType;
			try {
				eventType = Class.forName(entry.getKey());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			Object event = jsonSerializer.revert(entry.getValue(), eventType);
			list.add((IDomainEvent<?> )event);
		}
		return list;
	}

}
