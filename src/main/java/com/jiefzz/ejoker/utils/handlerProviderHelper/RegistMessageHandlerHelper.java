package com.jiefzz.ejoker.utils.handlerProviderHelper;

import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.MessageHandler;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.varieties.ApplicationMessageHandler;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.varieties.DomainEventStreamMessageHandler;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.varieties.PublishableExceptionMessageHandler;

public final class RegistMessageHandlerHelper {

	static public void checkAndRegistMessageHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(MessageHandler.class) ||
				clazz.isAnnotationPresent(ApplicationMessageHandler.class) ||
				clazz.isAnnotationPresent(DomainEventStreamMessageHandler.class) ||
				clazz.isAnnotationPresent(PublishableExceptionMessageHandler.class))
			MessageHandlerPool.regist((Class<? extends IMessageHandler> )clazz);
	}
	
}
