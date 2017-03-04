package com.jiefzz.ejoker.utils.handlerProviderHelper;

import com.jiefzz.ejoker.domain.AbstractAggregateRoot;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.AggregateRootHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.AggregateRoot;

public class RegistDomainEventHandlerHelper {

	static public void checkAndRegistDomainEventHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(AggregateRoot.class))
			AggregateRootHandlerPool.regist((Class<? extends AbstractAggregateRoot<?>> )clazz);
	}

}
