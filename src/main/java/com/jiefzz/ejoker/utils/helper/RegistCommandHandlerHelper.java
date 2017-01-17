package com.jiefzz.ejoker.utils.helper;

import com.jiefzz.ejoker.domain.AbstractAggregateRoot;
import com.jiefzz.ejoker.domain.helper.AggregateHandlerJavaHelper;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.CommandHandler;

public final class RegistCommandHandlerHelper {

	static public void checkAndRegistCommandHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(CommandHandler.class))
			AggregateHandlerJavaHelper.regist((Class<? extends AbstractAggregateRoot<?>> )clazz);
	}
	
}
