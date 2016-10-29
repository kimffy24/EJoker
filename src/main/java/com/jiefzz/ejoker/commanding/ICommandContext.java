package com.jiefzz.ejoker.commanding;

import com.jiefzz.ejoker.domain.IAggregateRoot;

public interface ICommandContext {

    public void add(IAggregateRoot aggregateRoot);
    
    /**
     * Because Java's generic type is pseudo generic type, not real generic.
     * T will convert to Object in running. It means we will lost the type info
     * while program running.
     * we have to pass the type information.
     * @param id
     * @param tryFromCache
     * @return
     */
    public <T extends IAggregateRoot> T get(Object id, Class<T> clazz, boolean tryFromCache);
    
    /**
     * @see com.jiefzz.ejoker.commanding.ICommandContext.get(Object, Class, boolean)
     * @param id
     * @param clazz
     * @return
     */
    public <T extends IAggregateRoot> T get(Object id, Class<T> clazz);
    
    /**
     * Not like C#, T will lost while program running. Please use get(Object, Class, boolean).
     * @deprecated
     * @see com.jiefzz.ejoker.commanding.ICommandContext.get(Object, Class, boolean)
     * @param id
     * @param tryFromCache
     * @return
     */
    public <T extends IAggregateRoot> T get(Object id, boolean tryFromCache);
    
    /**
     * Not like C#, T will lost while program running.. Please use get(Object, Class).
     * @deprecated
     * @see com.jiefzz.ejoker.commanding.ICommandContext.get(Object, Class, boolean)
     * @param id
     * @return
     */
    public <T extends IAggregateRoot> T get(Object id);
    
    public void setResult(String result);
    public String getResult();
    
}
