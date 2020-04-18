package pro.jk.ejoker.queue;

import java.util.Set;

public interface ITopicProvider<T> {
	
	String getTopic(T source);
	
    Set<String> GetAllTopics();
    
}
