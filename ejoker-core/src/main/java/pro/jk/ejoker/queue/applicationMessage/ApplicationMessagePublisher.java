package pro.jk.ejoker.queue.applicationMessage;

import java.nio.charset.Charset;

import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.infrastructure.ITypeNameProvider;
import pro.jk.ejoker.messaging.IApplicationMessage;
import pro.jk.ejoker.queue.ITopicProvider;
import pro.jk.ejoker.queue.QueueMessageTypeCode;
import pro.jk.ejoker.queue.SendQueueMessageService.SendServiceContext;
import pro.jk.ejoker.queue.skeleton.AbstractEJokerQueueProducer;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;

@EService
public class ApplicationMessagePublisher extends AbstractEJokerQueueProducer<IApplicationMessage> {

	@Dependence
	private ITopicProvider<IApplicationMessage> messageTopicProvider;
	
	@Dependence
	private IJSONStringConverterPro jsonConverter;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Override
	protected SendServiceContext createEQueueMessage(IApplicationMessage message) {
		String topic = messageTopicProvider.getTopic(message);
		String data = jsonConverter.convert(message);

		return new SendServiceContext(this.getMessageType(message),
				this.getMessageClassDesc(message),
				new EJokerQueueMessage(
						topic,
						QueueMessageTypeCode.ApplicationMessage.ordinal(),
						data.getBytes(Charset.forName("UTF-8")),
						typeNameProvider.getTypeName(message.getClass())),
				data,
				this.getRoutingKey(message),
				message.getId(),
				message.getItems());
	}

	@Override
	protected String getMessageType(IApplicationMessage message) {
		return "applicationMessage";
	}

	@Override
	protected String getRoutingKey(IApplicationMessage message) {
		return message.getId();
	}

}
