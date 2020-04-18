package pro.jk.ejoker.domain;

import java.util.concurrent.Future;

public interface IRepository {
	
    public Future<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId);
    
}
