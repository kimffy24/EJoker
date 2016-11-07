package com.jiefzz.ejoker.eventing;

import java.util.Collection;
import java.util.Map;

public interface IEventSerializer {

	/**
	 * serialize the given events to dictionary
	 * @param events
	 * @return
	 */
	public Map<String, String> serializer(Collection<IDomainEvent<?>> events);
	
	/**
	 * deserialize the given data to events
	 * @param data
	 * @return
	 */
	public Collection<IDomainEvent<?>> deserializer(Map<String, String> data);
	
}
