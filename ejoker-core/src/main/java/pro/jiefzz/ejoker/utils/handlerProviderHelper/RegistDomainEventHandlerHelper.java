package pro.jiefzz.ejoker.utils.handlerProviderHelper;

import pro.jiefzz.ejoker.domain.AbstractAggregateRoot;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.AggregateRootHandlerPool;
import pro.jiefzz.ejoker.z.context.annotation.assemblies.AggregateRoot;

public class RegistDomainEventHandlerHelper {

	static public void checkAndRegistDomainEventHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(AggregateRoot.class))
			AggregateRootHandlerPool.regist((Class<? extends AbstractAggregateRoot<?>> )clazz);
	}

}
