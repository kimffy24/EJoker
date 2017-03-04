package com.jiefzz.ejoker.eventing;

import java.util.List;
import java.util.Map;

public interface IEventSerializer {

	/**
	 * serialize the given events to dictionary
	 * @param events
	 * @return
	 */
	public Map<String, String> serializer(List<IDomainEvent<?>> events);
	
	/**
	 * deserialize the given data to events
	 * @param data
	 * @return
	 */
	public List<IDomainEvent<?>> deserializer(Map<String, String> data);
	
}
