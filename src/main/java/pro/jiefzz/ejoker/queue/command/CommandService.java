package pro.jiefzz.ejoker.queue.command;

import static pro.jiefzz.ejoker.z.system.extension.LangUtil.await;

import java.nio.charset.Charset;

import pro.jiefzz.ejoker.commanding.CommandResult;
import pro.jiefzz.ejoker.commanding.CommandReturnType;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandService;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.SendQueueMessageService;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IProducerWrokerAware;
import pro.jiefzz.ejoker.z.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.z.context.annotation.context.EService;
import pro.jiefzz.ejoker.z.service.IJSONConverter;
import pro.jiefzz.ejoker.z.service.IWorkerService;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.EJokerFutureTaskUtil;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.RipenFuture;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapper;
import pro.jiefzz.ejoker.z.system.extension.acrossSupport.SystemFutureWrapperUtil;
import pro.jiefzz.ejoker.z.task.AsyncTaskResult;
import pro.jiefzz.ejoker.z.task.AsyncTaskStatus;
import pro.jiefzz.ejoker.z.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.z.utils.Ensure;

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
	private CommandResultProcessor commandResultProcessor;
	
	@Dependence
	private ITypeNameProvider typeNameProvider;

	private IProducerWrokerAware producer;

	public CommandService useProducer(IProducerWrokerAware producer) {
		this.producer = producer;
		return this;
	}
	
	public CommandService start() {
		try {
			producer.start();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return this;
	}

	public CommandService shutdown() {
		try {
			producer.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this;
	}
	
	@Override
	public SystemFutureWrapper<AsyncTaskResult<Void>> sendAsync(final ICommand command) {
		try {
			return sendQueueMessageService.sendMessageAsync(
					producer,
					"command",
					command.getClass().getSimpleName(),
					buildCommandMessage(command),
					command.getAggregateRootId(),
					command.getId(),
					command.getItems());
		} catch (RuntimeException ex) {
			return new SystemFutureWrapper<>(EJokerFutureTaskUtil.newFutureTask(AsyncTaskStatus.Failed, ex.getMessage()));
		}
	}

	/**
	 * 此方法实现起来比较绕，援引自C# ENode.EQueue.CommandService.executeAsync<br>
	 * Java中没有c# 的 async/await 调用，只能用最原始的创建线程任务对象的方法。
	 */
	@Override
	public SystemFutureWrapper<AsyncTaskResult<CommandResult>> executeAsync(final ICommand command, final CommandReturnType commandReturnType) {

		Ensure.notNull(commandResultProcessor, "commandResultProcessor");

		RipenFuture<AsyncTaskResult<CommandResult>> remoteTaskCompletionSource = new RipenFuture<>();
		commandResultProcessor.regiesterProcessingCommand(command, commandReturnType, remoteTaskCompletionSource);
		
		AsyncTaskResult<Void> sendResult = await(systemAsyncHelper.submit(() -> {
			
			return await(sendQueueMessageService.sendMessageAsync(
					producer,
					"command",
					command.getClass().getSimpleName(),
					buildCommandMessage(command, true),
					command.getAggregateRootId(),
					command.getId(),
					command.getItems()));
		}));
		
		if(AsyncTaskStatus.Success.equals(sendResult.getStatus())) {
			return new SystemFutureWrapper<>(remoteTaskCompletionSource);
		} else {
			commandResultProcessor.processFailedSendingCommand(command);
			return new SystemFutureWrapper<>(EJokerFutureTaskUtil.newFutureTask(sendResult.getStatus(), sendResult.getErrorMessage(), CommandResult.class));
		}
		
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
        String tag = typeNameProvider.getTypeName(command.getClass());
        return new EJokerQueueMessage(
            topic,
            QueueMessageTypeCode.CommandMessage.ordinal(),
            messageData.getBytes(Charset.forName("UTF-8")),
            tag);
	}
}