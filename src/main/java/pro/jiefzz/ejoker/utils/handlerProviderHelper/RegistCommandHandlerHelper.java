package pro.jiefzz.ejoker.utils.handlerProviderHelper;

import pro.jiefzz.ejoker.commanding.AbstractCommandHandler;
import pro.jiefzz.ejoker.utils.handlerProviderHelper.containers.CommandHandlerPool;
import pro.jiefzz.ejoker.z.context.annotation.context.ESType;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.context.dev2.IEjokerContextDev2;

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
