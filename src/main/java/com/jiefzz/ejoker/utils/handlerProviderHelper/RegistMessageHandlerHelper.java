package com.jiefzz.ejoker.utils.handlerProviderHelper;

import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.MessageHandler;

public final class RegistMessageHandlerHelper {

	static public void checkAndRegistMessageHandler(Class<?> clazz) {
		if(clazz.isAnnotationPresent(MessageHandler.class))
			MessageHandlerPool.regist((Class<? extends IMessageHandler> )clazz);
	}
	
}
