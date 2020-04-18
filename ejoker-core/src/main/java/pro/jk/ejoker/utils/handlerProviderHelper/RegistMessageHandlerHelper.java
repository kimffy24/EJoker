package pro.jk.ejoker.utils.handlerProviderHelper;

import pro.jk.ejoker.common.context.annotation.context.ESType;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jk.ejoker.messaging.IMessageHandler;
import pro.jk.ejoker.utils.handlerProviderHelper.containers.MessageHandlerPool;

public final class RegistMessageHandlerHelper {

	static public void checkAndRegistMessageHandler(Class<?> clazz, MessageHandlerPool handlerPool, IEjokerContextDev2 ejokerContext) {

		if(clazz.isAnnotationPresent(EService.class)) {
			EService esa = clazz.getAnnotation(EService.class);
			ESType type = esa.type();
			if(ESType.MESSAGE_HANDLER.equals(type))
				handlerPool.regist((Class<? extends IMessageHandler> )clazz, () -> ejokerContext);
		}
	}
	
}
