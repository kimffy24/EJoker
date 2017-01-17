package com.jiefzz.ejoker.utils.helper;

import com.jiefzz.ejoker.domain.AbstractAggregateRoot;
import com.jiefzz.ejoker.domain.helper.AggregateHandlerJavaHelper;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.Domain;

public class RegistDomainEventHandlerHelper {

	static public void checkAndRegistDomainEventHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(Domain.class))
			AggregateHandlerJavaHelper.regist((Class<? extends AbstractAggregateRoot<?>> )clazz);
	}

}
