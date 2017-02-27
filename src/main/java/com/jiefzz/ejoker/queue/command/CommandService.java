package com.jiefzz.ejoker.queue.command;

import java.nio.charset.Charset;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.CommandExecuteTimeoutException;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandRoutingKeyProvider;
import com.jiefzz.ejoker.commanding.ICommandService;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.queue.ITopicProvider;
import com.jiefzz.ejoker.queue.QueueMessageTypeCode;
import com.jiefzz.ejoker.queue.SendQueueMessageService;
import com.jiefzz.ejoker.queue.skeleton.IQueueProducerWokerService;
import com.jiefzz.ejoker.queue.skeleton.clients.producer.IProducer;
import com.jiefzz.ejoker.queue.skeleton.prototype.EJokerQueueMessage;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.system.extension.FutureTaskCompletionSource;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.task.AsyncPool;
import com.jiefzz.ejoker.z.common.task.IAsyncTask;
import com.jiefzz.ejoker.z.common.task.ThreadPoolMaster;
import com.jiefzz.ejoker.z.common.utilities.Ensure;

/**
 * @author JiefzzLon
 *
 */
@EService
public class CommandService implements ICommandService, IQueueProducerWokerService {

	final static Logger logger = LoggerFactory.getLogger(CommandService.class);
	
	/**
	 * TODO 辅助实现 async/await 调用
	 */
//	private AsyncPool asyncPool = ThreadPoolMaster.getPoolInstance(CommandService.class);
	
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

	private IProducer producer;

	public CommandService useProducer(IProducer producer) { this.producer = producer; return this;}
	public IProducer getProducer() { return producer; }

	@Override
	public CommandService start() {
		producer.start();
		return this;
	}

	@Override
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
			RipenFuture<AsyncTaskResultBase> ripenFuture = new RipenFuture<AsyncTaskResultBase>();
			ripenFuture.trySetResult(taskResult);
			return ripenFuture;
		}
	}

	@Override
	public void send(final ICommand command) {
		sendQueueMessageService.sendMessage(producer, buildCommandMessage(command), commandRouteKeyProvider.getRoutingKey(command));
	}

	@Override
	public CommandResult execute(final ICommand command, int timeoutMillis) {
		return execute(command, CommandReturnType.CommandExecuted, timeoutMillis);
	}

	@Override
	public CommandResult execute(final ICommand command, final CommandReturnType commandReturnType, final int timeoutMillis) {
		Future<AsyncTaskResult<CommandResult>> executeAsync = executeAsync(command, commandReturnType);
		AsyncTaskResult<CommandResult> asyncTaskResult;
		try {
			asyncTaskResult = executeAsync.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			logger.error("Command execute timeout! [commandId={}, aggregateRootId={}]", command.getId(), command.getAggregateRootId());
			throw new CommandExecuteTimeoutException(e);
		}
		return asyncTaskResult.getData();
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
		final FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> remoteTaskCompletionSource = new FutureTaskCompletionSource<AsyncTaskResult<CommandResult>>();
		commandResultProcessor.regiesterProcessingCommand(command, commandReturnType, remoteTaskCompletionSource);
		final RipenFuture<AsyncTaskResult<CommandResult>> localTask = new RipenFuture<AsyncTaskResult<CommandResult>>();
		
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					AsyncTaskResultBase result = sendQueueMessageService.sendMessageAsync(producer, buildCommandMessage(command, true), commandRouteKeyProvider.getRoutingKey(command)).get();
				if(AsyncTaskStatus.Success == result.getStatus()) {
					localTask.trySetResult(remoteTaskCompletionSource.task.get());
				} else {
					commandResultProcessor.processFailedSendingCommand(command);
					localTask.trySetResult(new AsyncTaskResult<CommandResult>(result.getStatus(), result.getErrorMessage()));
				}
			} catch ( Exception e ) {
				localTask.trySetResult(new AsyncTaskResult<CommandResult>(AsyncTaskStatus.Failed, e.getMessage()));
			}
			}}).start();
		return localTask;
		
//		return asyncPool.execute(
//				new IAsyncTask<AsyncTaskResult<CommandResult>>() {
//					@Override
//					public AsyncTaskResult<CommandResult> call() throws Exception {
//						try {
//							Ensure.notNull(commandResultProcessor, "commandResultProcessor");
//							FutureTaskCompletionSource<AsyncTaskResult<CommandResult>> taskCompletionSource = new FutureTaskCompletionSource<AsyncTaskResult<CommandResult>>();
//							commandResultProcessor.regiesterProcessingCommand(command, commandReturnType, taskCompletionSource);
//							
//							AsyncTaskResultBase result = sendQueueMessageService.sendMessageAsync(producer, buildCommandMessage(command, true), commandRouteKeyProvider.getRoutingKey(command)).get();
//							if(AsyncTaskStatus.Success == result.getStatus())
//								return taskCompletionSource.task.get();
//							commandResultProcessor.processFailedSendingCommand(command);
//							return new AsyncTaskResult<CommandResult>(result.getStatus(), result.getErrorMessage());
//						} catch ( Exception e ) {
//							return new AsyncTaskResult<CommandResult>(AsyncTaskStatus.Failed, e.getMessage());
//						}
//					}
//					
//				}
//		);
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