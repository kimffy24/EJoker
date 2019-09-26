package pro.jiefzz.ejoker.domain;

import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;

public interface IRepository {
	
    public SystemFutureWrapper<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId);
    
}
