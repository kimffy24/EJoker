package com.jiefzz.ejoker.queue.command;

import java.nio.charset.Charset;

import org.apache.rocketmq.client.exception.MQClientException;

import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandRoutingKeyProvider;
import com.jiefzz.ejoker.commanding.ICommandService;
import com.jiefzz.ejoker.queue.ITopicProvider;
import com.jiefzz.ejoker.queue.QueueMessageTypeCode;
import com.jiefzz.ejoker.queue.SendQueueMessageService;
import com.jiefzz.ejoker.queue.completation.DefaultMQProducer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.extension.FutureTaskCompletionSource;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.Ensure;

/**
 * @author JiefzzLon
 *
 */
@EService
public class CommandService implements ICommandService, IWorkerService {

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;
	
	/**
	 * all command will send by this object.
	 */
	@Dependence
	private SendQueueMessageService sendQueueMessageService;
	
	@Dependence
	private IJSONConverter jsonConverter;
	
	@Dependence
	private ITopicProvider<ICommand> commandTopicProvider;
	
	@Dependence
	private ICommandRoutingKeyProvider commandRouteKeyProvider;
	
	@Dependence
	private CommandResultProcessor commandResultProcessor;

	private DefaultMQProducer producer;

	public CommandService useProducer(DefaultMQProducer producer) {
		this.producer = producer;
		return this;
	}
	
	public DefaultMQProducer getProducer() {
		return producer;
	}

	public CommandService start() {
		try {
			producer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return this;
	}

	public CommandService shutdown() {
		producer.shutdown();
		return this;
	}
	
	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> sendAsync(final ICommand command) {
		return sendQueueMessageService.sendMessageAsync(producer, buildCommandMessage(command), commandRouteKeyProvider.getRoutingKey(command));
	}

	@Override
	public SystemFutureWrapper<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command) {
		return executeAsync(command, CommandReturnType.CommandExecuted);
	}

	/**
	 * 此方法实现起来比较绕，援引自C# ENode.EQueue.CommandService.executeAsync<br>
	 * Java中没有c# 的 async/await 调用，只能用最原始的创建线程任务对象的方法。
	 */
	@Override
	public SystemFutureWrapper<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command, final CommandReturnType commandReturnType) {

		Ensure.notNull(commandResultProcessor, "commandResultProcessor");
		

		FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> remoteTaskCompletionSource = new FutureTaskCompletionSource<>();
		commandResultProcessor.regiesterProcessingCommand(command, commandReturnType, remoteTaskCompletionSource);
		SystemFutureWrapper<AsyncTaskResult<Void>> sendMessageAsync = sendQueueMessageService.sendMessageAsync(producer, buildCommandMessage(command, true), commandRouteKeyProvider.getRoutingKey(command));
		
		/// 如果这里能用协程，会更好，netty有吗？
		/// TODO 一个优化点
		return systemAsyncHelper.submit(() -> {
					AsyncTaskResult<Void> result = sendMessageAsync.get();
					if(AsyncTaskStatus.Success.equals(result.getStatus())) {
						// 此线程会在这了盲等！
						return remoteTaskCompletionSource.task.get();
					} else {
						commandResultProcessor.processFailedSendingCommand(command);
						throw new RuntimeException(result.getErrorMessage());
					}
			}
		);
		
	}

	private EJokerQueueMessage buildCommandMessage(ICommand command){
		return buildCommandMessage(command, false);
	}

	private EJokerQueueMessage buildCommandMessage(ICommand command, boolean needReply){
		Ensure.notNull(command.getAggregateRootId(), "aggregateRootId");
        String commandData = jsonConverter.convert(command);
        String topic = commandTopicProvider.getTopic(command);
        String replyAddress = needReply && (null!=commandResultProcessor) ? commandResultProcessor.getBindingAddress() : null;
        String messageData = jsonConverter.convert(new CommandMessage(commandData, replyAddress));
        return new EJokerQueueMessage(
            topic,
            QueueMessageTypeCode.CommandMessage.ordinal(),
            messageData.getBytes(Charset.forName("UTF-8")),
            command.getClass().getName());
	}
}