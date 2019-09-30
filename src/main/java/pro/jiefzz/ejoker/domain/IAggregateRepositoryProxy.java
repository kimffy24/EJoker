package pro.jiefzz.ejoker.domain;

import java.util.concurrent.Future;

import pro.jiefzz.ejoker.infrastructure.IObjectProxy;

public interface IAggregateRepositoryProxy extends IObjectProxy {

	public Future<IAggregateRoot> getAsync(String aggregateRootId);
	
}
