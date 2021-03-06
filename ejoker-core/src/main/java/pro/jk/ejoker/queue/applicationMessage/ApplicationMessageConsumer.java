package pro.jk.ejoker.queue.applicationMessage;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.infrastructure.ITypeNameProvider;
import pro.jk.ejoker.messaging.IApplicationMessage;
import pro.jk.ejoker.messaging.IMessageDispatcher;
import pro.jk.ejoker.queue.skeleton.AbstractEJokerQueueConsumer;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jk.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;

@EService
public class ApplicationMessageConsumer extends AbstractEJokerQueueConsumer {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationMessageConsumer.class);

	@Dependence
	private IJSONStringConverterPro jsonSerializer;

	@Dependence
	private IMessageDispatcher messageDispatcher;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Override
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		
		String applicationMessageString = new String(queueMessage.getBody(), Charset.forName("UTF-8"));
		logger.debug("Received application message. [queueMessage: {}, body: {}]", queueMessage, applicationMessageString);
		
        Class<? extends IApplicationMessage> applicationMessageType = (Class<? extends IApplicationMessage> )typeNameProvider.getType(queueMessage.getTag());
        IApplicationMessage message = jsonSerializer.revert(applicationMessageString, applicationMessageType);

        systemAsyncHelper.submit(() -> {
        	await(messageDispatcher.dispatchMessageAsync(message));
        	context.onMessageHandled(queueMessage);
        });
        
	}

	@Override
	protected long getConsumerLoopInterval() {
		return 4000l;
	}
}
