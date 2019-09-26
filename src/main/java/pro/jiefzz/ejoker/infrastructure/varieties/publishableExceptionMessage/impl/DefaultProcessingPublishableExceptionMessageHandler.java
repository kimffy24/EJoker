package pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.impl;

import pro.jiefzz.ejoker.infrastructure.impl.AbstractDefaultProcessingMessageHandler;
import pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.IPublishableException;
import pro.jiefzz.ejoker.infrastructure.varieties.publishableExceptionMessage.ProcessingPublishableExceptionMessage;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;

/**
 * 
 * * 编写本类的目的是为了<br >
 * * 在上下文中不使用泛型注入，而是用推演
 * @author kimffy
 *
 */
@EService
public class DefaultProcessingPublishableExceptionMessageHandler extends AbstractDefaultProcessingMessageHandler<ProcessingPublishableExceptionMessage, IPublishableException> {

}
