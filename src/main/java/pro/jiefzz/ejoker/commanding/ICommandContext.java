package pro.jiefzz.ejoker.commanding;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.domain.IAggregateRoot;
import pro.jiefzz.ejoker.messaging.IApplicationMessage;

public interface ICommandContext {

	/**
	 * Add a new aggregate into the current command context.
	 * @param aggregateRoot
	 */
    public void add(IAggregateRoot aggregateRoot);

	/**
	 * Add a new aggregate into the current command context.
	 * @param aggregateRoot
	 */
    public Future<Void> addAsync(IAggregateRoot aggregateRoot);
    
    /**
     * Because Java's generic type is pseudo generic type, not real generic.
     * T will convert to Object in running. It means we will lost the type info
     * while program running.
     * we have to pass the type information.
     * @param id
     * @param tryFromCache
     * @return
     */
    public <T extends IAggregateRoot> Future<T> getAsync(Object id, Class<T> clazz, boolean tryFromCache);
    
    /**
     * @see com.jiefzz.ejoker.commanding.ICommandContext.get(Object, Class, boolean)
     * @param id
     * @param clazz
     * @return
     */
    default public <T extends IAggregateRoot> Future<T> getAsync(Object id, Class<T> clazz) {
    	return getAsync(id, clazz, true);
    }
    
    /**
     * 
     * @param result
     */
    public void setResult(String result);
    
    public String getResult();
    
    /**
     * Set an application message.
     * @param applicationMessage
     */
    public void setApplicationMessage(IApplicationMessage applicationMessage);
    
    /**
     * Get an application message.
     * @return
     */
    public IApplicationMessage getApplicationMessage();
    
}
