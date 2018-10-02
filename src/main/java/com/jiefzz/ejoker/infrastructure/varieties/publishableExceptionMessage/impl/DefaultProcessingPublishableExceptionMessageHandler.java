package com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.impl;

import com.jiefzz.ejoker.infrastructure.impl.DefaultProcessingMessageHandlerA;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import com.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.ProcessingPublishableExceptionMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;

/**
 * 
 * * 编写本类的目的是为了<br >
 * * 在上下文中不使用泛型注入，而是用推演
 * @author kimffy
 *
 */
@EService
public class DefaultProcessingPublishableExceptionMessageHandler extends DefaultProcessingMessageHandlerA<ProcessingPublishableExceptionMessage, IPublishableException> {

}
