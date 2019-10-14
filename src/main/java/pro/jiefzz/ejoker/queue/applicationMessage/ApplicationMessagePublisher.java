package pro.jiefzz.ejoker.queue.applicationMessage;

import java.nio.charset.Charset;

import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.messaging.IApplicationMessage;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueProducer;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;

@EService
public class ApplicationMessagePublisher extends AbstractEJokerQueueProducer<IApplicationMessage> {

	@Dependence
	private ITopicProvider<IApplicationMessage> messageTopicProvider;
	
	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Override
	protected EJokerQueueMessage createEQueueMessage(IApplicationMessage message) {
		String topic = messageTopicProvider.getTopic(message);
		String data = jsonConverter.convert(message);
		return new EJokerQueueMessage(topic, QueueMessageTypeCode.ApplicationMessage.ordinal(),
				data.getBytes(Charset.forName("UTF-8")), typeNameProvider.getTypeName(message.getClass()));
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
