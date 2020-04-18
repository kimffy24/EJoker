package pro.jk.ejoker.domain.impl;

import java.util.concurrent.Future;

import pro.jk.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jk.ejoker.domain.IAggregateRepository;
import pro.jk.ejoker.domain.IAggregateRepositoryProxy;
import pro.jk.ejoker.domain.IAggregateRoot;

/**
 * TODO 考虑更好的，更java的实现
 * 代理对象，用于获取仓储对象。
 * @author JiefzzLon
 */
public class AggregateRepositoryProxy implements IAggregateRepositoryProxy {

	private final IAggregateRepository<IAggregateRoot> aggregateRepository;

	public AggregateRepositoryProxy(IAggregateRepository<IAggregateRoot> aggregateRepository) {
		this.aggregateRepository = aggregateRepository;
	}

	@Override
	public Object getInnerObject() {
		return aggregateRepository;
	}

	@Override
	public Future<IAggregateRoot> getAsync(String aggregateRootId) {
		return EJokerFutureUtil.completeFuture(null);
	}

}
