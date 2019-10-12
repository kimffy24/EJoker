package pro.jiefzz.ejoker.queue.domainException;

import java.nio.charset.Charset;
import java.util.Map;

import pro.jiefzz.ejoker.domain.domainException.IDomainException;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueProducer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.utils.domainExceptionHelper.DomainExceptionCodecHelper;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;

@EService
public class DomainExceptionPublisher extends AbstractEJokerQueueProducer<IDomainException> {

	@Dependence
	private ITopicProvider<IDomainException> messageTopicProvider;
	
	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Override
	protected EJokerQueueMessage createEQueueMessage(IDomainException exception) {
		String topic = messageTopicProvider.getTopic(exception);
		final Map<String, String> serializableInfo = DomainExceptionCodecHelper.serialize(exception);
		DomainExceptionMessage pMsg = new DomainExceptionMessage();
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
	protected String getMessageType(IDomainException message) {
		return "exception";
	}

	@Override
	protected String getRoutingKey(IDomainException message) {
		return message.getId();
	}

}
