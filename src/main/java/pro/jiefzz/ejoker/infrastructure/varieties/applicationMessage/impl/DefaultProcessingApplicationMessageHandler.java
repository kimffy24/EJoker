package pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.impl;

import pro.jiefzz.ejoker.infrastructure.impl.AbstractDefaultProcessingMessageHandler;
import pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.IApplicationMessage;
import pro.jiefzz.ejoker.infrastructure.varieties.applicationMessage.ProcessingApplicationMessage;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;

/**
 * 
 * * 编写本类的目的是为了<br >
 * * 在上下文中不使用泛型注入，而是用推演
 * @author kimffy
 *
 */
@EService
public class DefaultProcessingApplicationMessageHandler extends AbstractDefaultProcessingMessageHandler<ProcessingApplicationMessage, IApplicationMessage> {

}
