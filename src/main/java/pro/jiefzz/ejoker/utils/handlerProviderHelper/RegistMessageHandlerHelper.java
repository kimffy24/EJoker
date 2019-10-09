package pro.jiefzz.ejoker.utils.handlerProviderHelper;

import pro.jiefzz.ejoker.infrastructure.messaging.IMessageHandler;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;
import pro.jiefzz.ejoker.z.context.annotation.assemblies.MessageHandler;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;

public final class RegistMessageHandlerHelper {

	static public void checkAndRegistMessageHandler(Class<?> clazz, IEjokerContextDev2 ejokerContext) {
		if(clazz.isAnnotationPresent(MessageHandler.class))
			MessageHandlerPool.regist((Class<? extends IMessageHandler> )clazz, () -> ejokerContext);
	}
	
}
