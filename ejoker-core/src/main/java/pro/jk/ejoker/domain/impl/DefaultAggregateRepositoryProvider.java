package pro.jk.ejoker.domain.impl;

import java.util.HashMap;
import java.util.Map;

import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.domain.IAggregateRepositoryProvider;
import pro.jk.ejoker.domain.IAggregateRepositoryProxy;

/**
 * AggregateRepository&lt;T extends IAggregateRoot&gt;其实可以通过eJoker上下文获得，<br />
 * 不用再自己实现一次获取过程，因为eJoker上下文已经干过这事了<br />
 * @author jiefzzLon
 * <br />
 * TODO 没做完！！！  把从上下文中获取 AggregateRepository&lt;T extends IAggregateRoot&gt; 的代码写出来<br />
 * TODO 没做完！！！  把从上下文中获取 AggregateRepository&lt;T extends IAggregateRoot&gt; 的代码写出来<br />
 *
 */
@EService
public class DefaultAggregateRepositoryProvider implements IAggregateRepositoryProvider {

	private Map<Class<?>, IAggregateRepositoryProxy> repositoryDict = 
			new HashMap<Class<?>, IAggregateRepositoryProxy>();

	public IAggregateRepositoryProxy getRepository(Class<?> aggregateRootClazz) {
		return repositoryDict.get(aggregateRootClazz);
	}

}
