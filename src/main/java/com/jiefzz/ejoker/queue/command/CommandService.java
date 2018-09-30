package com.jiefzz.ejoker.queue.command;

import java.nio.charset.Charset;
import java.util.concurrent.Future;

import org.apache.rocketmq.client.exception.MQClientException;

import com.jiefzz.ejoker.EJokerEnvironment;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandRoutingKeyProvider;
import com.jiefzz.ejoker.commanding.ICommandService;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.queue.ITopicProvider;
import com.jiefzz.ejoker.queue.QueueMessageTypeCode;
import com.jiefzz.ejoker.queue.SendQueueMessageService;
import com.jiefzz.ejoker.queue.completation.DefaultMQProducer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.extension.FutureTaskCompletionSource;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
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
	public Future<AsyncTaskResultBase> sendAsync(final ICommand command) {
		try {
			return sendQueueMessageService.sendMessageAsync(producer, buildCommandMessage(command), commandRouteKeyProvider.getRoutingKey(command));
		} catch ( Exception e ) {
			e.printStackTrace();
			AsyncTaskResultBase taskResult = new AsyncTaskResultBase(AsyncTaskStatus.Failed, e.getMessage());
			RipenFuture<AsyncTaskResultBase> ripenFuture = new RipenFuture<>();
			ripenFuture.trySetResult(taskResult);
			return ripenFuture;
		}
	}

	@Override
	public Future<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command) {
		return executeAsync(command, CommandReturnType.CommandExecuted);
	}

	/**
	 * 此方法实现起来比较绕，援引自C# ENode.EQueue.CommandService.executeAsync<br>
	 * Java中没有c# 的 async/await 调用，只能用最原始的创建线程任务对象的方法。
	 */
	@Override
	public Future<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command, final CommandReturnType commandReturnType) {

		Ensure.notNull(commandResultProcessor, "commandResultProcessor");
		final FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> remoteTaskCompletionSource = new FutureTaskCompletionSource<>();
		commandResultProcessor.regiesterProcessingCommand(command, commandReturnType, remoteTaskCompletionSource);
		final RipenFuture<AsyncTaskResult<CommandResult>> localTask = new RipenFuture<>();
		
		/// 如果这里能用协程，会更好，netty有吗？
		/// TODO 一个优化点
		systemAsyncHelper.submit(() -> {
				try {
					AsyncTaskResultBase result = sendQueueMessageService.sendMessageAsync(producer, buildCommandMessage(command, true), commandRouteKeyProvider.getRoutingKey(command)).get();
					if(AsyncTaskStatus.Success == result.getStatus()) {
						localTask.trySetResult(remoteTaskCompletionSource.task.get());
					} else {
						commandResultProcessor.processFailedSendingCommand(command);
						localTask.trySetResult(new AsyncTaskResult<>(result.getStatus(), result.getErrorMessage()));
					}
				} catch ( Exception e ) {
					localTask.trySetResult(new AsyncTaskResult<>(AsyncTaskStatus.Failed, e.getMessage()));
				}
			}
		);
		
		return localTask;
		
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