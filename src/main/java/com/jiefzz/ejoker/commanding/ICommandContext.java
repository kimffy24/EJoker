package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;

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
    public SystemFutureWrapper<AsyncTaskResult<Void>> addAsync(IAggregateRoot aggregateRoot);
    
    /**
     * Because Java's generic type is pseudo generic type, not real generic.
     * T will convert to Object in running. It means we will lost the type info
     * while program running.
     * we have to pass the type information.
     * @param id
     * @param tryFromCache
     * @return
     */
    public <T extends IAggregateRoot> SystemFutureWrapper<T> getAsync(Object id, Class<T> clazz, boolean tryFromCache);
    
    /**
     * @see com.jiefzz.ejoker.commanding.ICommandContext.get(Object, Class, boolean)
     * @param id
     * @param clazz
     * @return
     */
    default public <T extends IAggregateRoot> SystemFutureWrapper<T> getAsync(Object id, Class<T> clazz) {
    	return getAsync(id, clazz, true);
    }

    public <T extends IAggregateRoot> T get(Object id, Class<T> clazz, boolean tryFromCache);
    
    /**
     * @see com.jiefzz.ejoker.commanding.ICommandContext.get(Object, Class, boolean)
     * @param id
     * @param clazz
     * @return
     */
    default public <T extends IAggregateRoot> T get(Object id, Class<T> clazz) {
    	return get(id, clazz, true);
    }
    
    /**
     * 
     * @param result
     */
    public void setResult(String result);
    
    public String getResult();
    
}
