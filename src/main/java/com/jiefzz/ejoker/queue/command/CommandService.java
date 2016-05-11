package com.jiefzz.ejoker.queue.command;

import java.nio.charset.Charset;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandService;
import com.jiefzz.ejoker.commanding.ICommandTopicProvider;
import com.jiefzz.ejoker.context.annotation.context.EService;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.infrastructure.z.common.io.BaseAsyncTaskResult;
import com.jiefzz.ejoker.infrastructure.z.common.utilities.Ensure;
import com.jiefzz.ejoker.infrastructure.z.queue.clients.producers.IProducer;
import com.jiefzz.ejoker.infrastructure.z.queue.protocols.Message;
import com.jiefzz.ejoker.queue.QueueMessageTypeCode;
import com.jiefzz.ejoker.queue.SendQueueMessageService;

/**
 * @author JiefzzLon
 *
 */
@EService
public class CommandService implements ICommandService {

	/**
	 * all command will send by this object.
	 */
	@Resource
	private SendQueueMessageService sendQueueMessageService;
	@Resource
	IJSONConverter jsonConverter;
	@Resource
	IProducer producer;
	@Resource
	ICommandTopicProvider commandTopicProvider;
	
	@Override
	public Future<BaseAsyncTaskResult> sendAsync(ICommand command) {
		return sendQueueMessageService.sendMessageAsync(producer, buildCommandMessage(command), getRoutingKey(command));
	}

	@Override
	public void send(ICommand command) {
		sendQueueMessageService.sendMessage(producer, buildCommandMessage(command), getRoutingKey(command));
	}

	@Override
	public void execute(ICommand command, int timeoutMillis) {
		// TODO Auto-generated method stub

	}

	@Override
	public void execute(ICommand command, CommandReturnType commandReturnType, int timeoutMillis) {
		// TODO Auto-generated method stub

	}

	@Override
	public void executeAsync(ICommand command) {
		// TODO Auto-generated method stub

	}

	@Override
	public void executeAsync(ICommand command, CommandReturnType commandReturnType) {
		// TODO Auto-generated method stub

	}

	private Message buildCommandMessage(ICommand command){
		return buildCommandMessage(command, false);
	}

	private Message buildCommandMessage(ICommand command, boolean needReply){
		Ensure.notNull(command.getAggregateRootId(), "aggregateRootId");
        String commandData = jsonConverter.convert(command);
        String topic = commandTopicProvider.getTopic(command);
        String replyAddress = ""; //needReply && _commandResultProcessor != null ? _commandResultProcessor.BindingAddress.ToString() : null;
        String messageData = jsonConverter.convert(new CommandMessage(commandData, replyAddress));
        return new Message(
            topic,
            QueueMessageTypeCode.CommandMessage.ordinal(),
            messageData.getBytes(Charset.forName("UTF-8")),
            command.getClass().getName());
	}
	
	private String getRoutingKey(ICommand command) {
		return "cmd."+command.getAggregateRootId();
	}
}
