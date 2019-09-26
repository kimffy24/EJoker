package pro.jiefzz.ejoker.eventing;

import java.util.Collection;
import java.util.List;
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
	public List<IDomainEvent<?>> deserializer(Map<String, String> data);
	
}
