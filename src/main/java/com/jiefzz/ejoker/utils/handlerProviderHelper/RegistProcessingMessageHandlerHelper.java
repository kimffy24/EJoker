//package com.jiefzz.ejoker.utils.handlerProviderHelper;
//
//import com.jiefzz.ejoker.infrastructure.IProcessingMessageHandler;
//import com.jiefzz.ejoker.utils.handlerProviderHelper.containers.ProcessingMessageHandlerPool;
//
//public final class RegistProcessingMessageHandlerHelper {
//
//	static public void checkAndRegistProcessingMessageHandler(Class<?> clazz) {
//		if(IProcessingMessageHandler.class.isAssignableFrom(clazz))
//			ProcessingMessageHandlerPool.regist((Class<? extends IProcessingMessageHandler<?, ?>> )clazz);
//	}
//	
//}
