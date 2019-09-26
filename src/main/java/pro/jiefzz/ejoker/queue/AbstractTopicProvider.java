package pro.jiefzz.ejoker.queue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AbstractTopicProvider<T> implements ITopicProvider<T> {

	private final Map<Class<?>, String> topicDict = new HashMap<>();
	
	private final Set<String> allTopics = new HashSet<>();

	@Override
	public String getTopic(T source) {
		return topicDict.get(source.getClass());
	}

	@Override
	public Set<String> GetAllTopics() {
		return allTopics;
	}

	protected Set<Class<?>> getAllTypes() {
		return topicDict.keySet();
	}
	
	protected void registerTopic(String topic, Class<?>[] types) {
		for(Class<?> type : types)
			topicDict.put(type, topic);
		if(!allTopics.contains(topic)) allTopics.add(topic);
	}
}
