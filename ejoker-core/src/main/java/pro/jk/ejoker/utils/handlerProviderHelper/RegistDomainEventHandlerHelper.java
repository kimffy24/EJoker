package pro.jk.ejoker.utils.handlerProviderHelper;

import pro.jk.ejoker.common.context.annotation.assemblies.AggregateRoot;
import pro.jk.ejoker.domain.AbstractAggregateRoot;
import pro.jk.ejoker.utils.handlerProviderHelper.containers.AggregateRootHandlerPool;

public class RegistDomainEventHandlerHelper {

	static public void checkAndRegistDomainEventHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(AggregateRoot.class))
			AggregateRootHandlerPool.regist((Class<? extends AbstractAggregateRoot<?>> )clazz);
	}

}
