package pro.jiefzz.ejoker.queue.domainException;

import static pro.jiefzz.ejoker.common.system.extension.LangUtil.await;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.context.annotation.context.EService;
import pro.jiefzz.ejoker.common.service.IJSONConverter;
import pro.jiefzz.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.domain.domainException.IDomainException;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.messaging.IMessageDispatcher;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueConsumer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IEJokerQueueMessageContext;
import pro.jiefzz.ejoker.utils.domainExceptionHelper.DomainExceptionCodecHelper;

@EService
public class DomainExceptionConsumer extends AbstractEJokerQueueConsumer {

	private final static Logger logger = LoggerFactory.getLogger(DomainExceptionConsumer.class);

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
		
        Class<? extends IDomainException> exceptionType = (Class<? extends IDomainException> )typeNameProvider.getType(queueMessage.getTag());
        DomainExceptionMessage exceptionMessage = jsonSerializer.revert(new String(queueMessage.getBody(), Charset.forName("UTF-8")), DomainExceptionMessage.class);
        
        // TODO ???? 不对
        IDomainException exception = DomainExceptionCodecHelper.deserialize(exceptionMessage.getSerializableInfo(), exceptionType);
        exception.setItems(exceptionMessage.getItems());
        exception.setId(exceptionMessage.getUniqueId());
        exception.setTimestamp(exceptionMessage.getTimestamp());
        
        logger.debug(
        		"EJoker publishable message received, messageId: {}, exceptionType: {}",
        		exceptionMessage.getUniqueId(),
        		exceptionType.getSimpleName());
        systemAsyncHelper.submit(() -> {
        	await(messageDispatcher.dispatchMessageAsync(exception));
        	context.onMessageHandled(queueMessage);
        });
        
	}

	protected long getConsumerLoopInterval() {
		return 4000l;
	}
}
