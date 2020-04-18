package pro.jk.ejoker.domain;

import java.util.concurrent.Future;

import pro.jk.ejoker.infrastructure.IObjectProxy;

public interface IAggregateRepositoryProxy extends IObjectProxy {

	public Future<IAggregateRoot> getAsync(String aggregateRootId);
	
}
