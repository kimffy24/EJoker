package pro.jk.ejoker.domain.impl;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.util.concurrent.Future;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.system.exceptions.ArgumentNullException;
import pro.jk.ejoker.common.system.extension.acrossSupport.EJokerFutureUtil;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.domain.IAggregateRoot;
import pro.jk.ejoker.domain.IMemoryCache;
import pro.jk.ejoker.domain.IRepository;

@EService
public class DefaultRepository implements IRepository {

	@Dependence
	private IMemoryCache memoryCache;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Override
	public Future<IAggregateRoot> getAsync(Class<IAggregateRoot> aggregateRootType, Object aggregateRootId) {
		if (aggregateRootType == null)
			throw new ArgumentNullException("aggregateRootType");
		if (aggregateRootId == null)
			throw new ArgumentNullException("aggregateRootId");

		// TODO @await
		IAggregateRoot aggregateRoot = await(memoryCache.getAsync(aggregateRootId, aggregateRootType));
		if(null == aggregateRoot)
			aggregateRoot =  await(memoryCache.refreshAggregateFromEventStoreAsync(aggregateRootType, aggregateRootId.toString()));
		return EJokerFutureUtil.completeFuture(aggregateRoot);
		
	}

}
