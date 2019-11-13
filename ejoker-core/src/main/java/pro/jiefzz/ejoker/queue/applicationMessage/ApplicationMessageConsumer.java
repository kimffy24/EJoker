package pro.jiefzz.ejoker.queue.applicationMessage;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.messaging.IApplicationMessage;
import pro.jiefzz.ejoker.messaging.IMessageDispatcher;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueConsumer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;

@EService
public class ApplicationMessageConsumer extends AbstractEJokerQueueConsumer {

	private final static Logger logger = LoggerFactory.getLogger(ApplicationMessageConsumer.class);

	@Dependence
	private IJSONConverter jsonSerializer;

	@Dependence
	private IMessageDispatcher messageDispatcher;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;
	
	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Override
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		
        Class<? extends IApplicationMessage> applicationMessageType = (Class<? extends IApplicationMessage> )typeNameProvider.getType(queueMessage.getTag());
        IApplicationMessage message = jsonSerializer.revert(new String(queueMessage.getBody(), Charset.forName("UTF-8")), applicationMessageType);

        logger.debug("EJoker application message received, messageId: {}, messageType: {}", message.getId(), applicationMessageType.getSimpleName());
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
