package com.jiefzz.ejoker.utils.handlerProviderHelper;

import com.jiefzz.ejoker.infrastructure.IMessageHandler;
import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import com.jiefzz.ejoker.z.common.context.annotation.assemblies.MessageHandler;
import com.jiefzz.ejoker.z.common.context.dev2.IEjokerContextDev2;

public final class RegistMessageHandlerHelper {

	static public void checkAndRegistMessageHandler(IEjokerContextDev2 ejokerContext, Class<?> clazz) {
		if(clazz.isAnnotationPresent(MessageHandler.class))
			MessageHandlerPool.regist((Class<? extends IMessageHandler> )clazz, () -> ejokerContext);
	}
	
}
