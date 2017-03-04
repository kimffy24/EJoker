package com.jiefzz.ejoker.eventing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jiefzz.ejoker.eventing.IDomainEvent;
import com.jiefzz.ejoker.eventing.IEventSerializer;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.utils.JavaObjectSerializeUtil;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

@EService
public class DefaultEventSerializer implements IEventSerializer {

	@Dependence
	private IJSONConverter jsonSerializer;
	
	@Override
	public Map<String, String> serializer(List<IDomainEvent<?>> events) {
		Map<String, String> dict = new LinkedHashMap<String, String>();
		for(IDomainEvent<?> event:events)
			dict.put(event.getClass().getName(), JavaObjectSerializeUtil.serialize(event));
		return dict;
	}

	@Override
	public List<IDomainEvent<?>> deserializer(Map<String, String> data) {
		List<IDomainEvent<?>> list = new ArrayList<IDomainEvent<?>>();
		Set<Entry<String,String>> entrySet = data.entrySet();
		for(Entry<String,String> entry:entrySet) {
			Class<?> eventType;
			try {
				eventType = Class.forName(entry.getKey());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			Serializable event = JavaObjectSerializeUtil.deserialize(entry.getValue());
			list.add((IDomainEvent<?> )event);
		}
		return list;
	}

}
