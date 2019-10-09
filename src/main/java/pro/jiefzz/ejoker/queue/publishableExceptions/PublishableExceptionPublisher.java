package pro.jiefzz.ejoker.queue.publishableExceptions;

import java.nio.charset.Charset;
import java.util.Map;

import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.infrastructure.messaging.varieties.publishableException.IPublishableException;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueProducer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.utils.publishableExceptionHelper.PublishableExceptionCodecHelper;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;

@EService
public class PublishableExceptionPublisher extends AbstractEJokerQueueProducer<IPublishableException> {

	@Dependence
	private ITopicProvider<IPublishableException> messageTopicProvider;
	
	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Override
	protected EJokerQueueMessage createEQueueMessage(IPublishableException exception) {
		String topic = messageTopicProvider.getTopic(exception);
		final Map<String, String> serializableInfo = PublishableExceptionCodecHelper.serialize(exception);
		PublishableExceptionMessage pMsg = new PublishableExceptionMessage();
		{
			pMsg.setUniqueId(exception.getId());
			pMsg.setTimestamp(exception.getTimestamp());
			pMsg.setSerializableInfo(serializableInfo);
			pMsg.setItems(exception.getItems());
		}
		String data = jsonConverter.convert(pMsg);
		return new EJokerQueueMessage(topic, QueueMessageTypeCode.ExceptionMessage.ordinal(),
				data.getBytes(Charset.forName("UTF-8")), typeNameProvider.getTypeName(exception.getClass()));
	}

	@Override
	protected String getMessageType(IPublishableException message) {
		return "exception";
	}

	@Override
	protected String getRoutingKey(IPublishableException message) {
		return message.getId();
	}

}
