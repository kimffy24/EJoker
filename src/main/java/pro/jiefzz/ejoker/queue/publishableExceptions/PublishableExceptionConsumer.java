package pro.jiefzz.ejoker.queue.publishableExceptions;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.infrastructure.messaging.IMessageDispatcher;
import pro.jiefzz.ejoker.infrastructure.messaging.varieties.publishableException.IPublishableException;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueConsumer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;
import pro.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionCodecHelper;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.system.task.context.SystemAsyncHelper;

@EService
public class PublishableExceptionConsumer extends AbstractEJokerQueueConsumer {

	private final static Logger logger = LoggerFactory.getLogger(PublishableExceptionConsumer.class);

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
		
        Class<? extends IPublishableException> publishableExceptionType = (Class<? extends IPublishableException> )typeNameProvider.getType(queueMessage.getTag());
        PublishableExceptionMessage exceptionMessage = jsonSerializer.revert(new String(queueMessage.getBody(), Charset.forName("UTF-8")), PublishableExceptionMessage.class);
        
        // TODO ???? 不对
        IPublishableException exception = PublishableExceptionCodecHelper.deserialize(exceptionMessage.getSerializableInfo(), publishableExceptionType);

        logger.debug(
        		"EJoker publishable message received, messageId: {}, exceptionType: {}",
        		exceptionMessage.getUniqueId(),
        		publishableExceptionType.getSimpleName());
        systemAsyncHelper.submit(() -> {
        	await(messageDispatcher.dispatchMessageAsync(exception));
        	context.onMessageHandled(queueMessage);
        });
        
	}

	protected long getConsumerLoopInterval() {
		return 4000l;
	}
}
