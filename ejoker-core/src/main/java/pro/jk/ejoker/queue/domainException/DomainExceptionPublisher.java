package pro.jk.ejoker.queue.domainException;

import java.nio.charset.Charset;
import java.util.Map;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.domain.domainException.IDomainException;
import pro.jk.ejoker.infrastructure.ITypeNameProvider;
import pro.jk.ejoker.queue.ITopicProvider;
import pro.jk.ejoker.queue.QueueMessageTypeCode;
import pro.jk.ejoker.queue.SendQueueMessageService.SendServiceContext;
import pro.jk.ejoker.queue.skeleton.AbstractEJokerQueueProducer;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jk.ejoker.utils.domainExceptionHelper.DomainExceptionCodecHelper;

@EService
public class DomainExceptionPublisher extends AbstractEJokerQueueProducer<IDomainException> {

	@Dependence
	private ITopicProvider<IDomainException> messageTopicProvider;
	
	@Dependence
	private IJSONStringConverterPro jsonConverter;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Override
	protected SendServiceContext createEQueueMessage(IDomainException exception) {
		String topic = messageTopicProvider.getTopic(exception);
		final Map<String, String> serializableInfo = DomainExceptionCodecHelper.serialize(exception, false);
		DomainExceptionMessage pMsg = new DomainExceptionMessage();
		{
			pMsg.setUniqueId(exception.getId());
			pMsg.setTimestamp(exception.getTimestamp());
			pMsg.setSerializableInfo(serializableInfo);
			pMsg.setItems(exception.getItems());
		}
		String data = jsonConverter.convert(pMsg);

		return new SendServiceContext(this.getMessageType(exception),
				this.getMessageClassDesc(exception),
				new EJokerQueueMessage(
						topic,
						QueueMessageTypeCode.ExceptionMessage.ordinal(),
						data.getBytes(Charset.forName("UTF-8")),
						typeNameProvider.getTypeName(exception.getClass())),
				data,
				this.getRoutingKey(exception),
				exception.getId(),
				exception.getItems());
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
