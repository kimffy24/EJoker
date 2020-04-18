package pro.jk.ejoker.utils.handlerProviderHelper;

import pro.jk.ejoker.commanding.AbstractCommandHandler;
import pro.jk.ejoker.common.context.annotation.context.ESType;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jk.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;

public final class RegistCommandHandlerHelper {

	static public void checkAndRegistCommandAsyncHandler(Class<?> clazz, CommandHandlerPool commandAsyncHandlerPool, IEjokerContextDev2 ejokerContext) {

		if(clazz.isAnnotationPresent(EService.class)) {
			EService esa = clazz.getAnnotation(EService.class);
			ESType type = esa.type();
			if(ESType.COMMAND_HANDLER.equals(type))
				commandAsyncHandlerPool.regist((Class<? extends AbstractCommandHandler> )clazz, () -> ejokerContext);
		}
		
	}
	
}
