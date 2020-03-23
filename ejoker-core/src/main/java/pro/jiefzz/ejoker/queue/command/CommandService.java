package pro.jiefzz.ejoker.queue.command;

import static pro.jiefzz.ejoker.common.system.extension.LangUtil.await;

import java.nio.charset.Charset;
import java.util.concurrent.Future;

import pro.jiefzz.ejoker.commanding.CommandResult;
import pro.jiefzz.ejoker.commanding.CommandReturnType;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.commanding.ICommandService;
import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.context.annotation.context.EService;
import pro.jiefzz.ejoker.common.service.IJSONConverter;
import pro.jiefzz.ejoker.common.service.IWorkerService;
import pro.jiefzz.ejoker.common.system.extension.acrossSupport.RipenFuture;
import pro.jiefzz.ejoker.common.system.helper.Ensure;
import pro.jiefzz.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.QueueMessageTypeCode;
import pro.jiefzz.ejoker.queue.SendQueueMessageService;
import pro.jiefzz.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jiefzz.ejoker.queue.skeleton.aware.IProducerWrokerAware;

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
	public boolean isAllReady() {
		return true;
	}

	@Override
	public Future<Void> sendAsync(final ICommand command) {
		return sendQueueMessageService.sendMessageAsync(
				producer,
				"command",
				command.getClass().getSimpleName(),
				buildCommandMessage(command),
				command.getAggregateRootId(),
				command.getId(),
				command.getItems());
	}

	/**
	 * 此方法实现起来比较绕，援引自C# ENode.EQueue.CommandService.executeAsync<br>
	 * Java中没有c# 的 async/await 调用，只能用最原始的创建线程任务对象的方法。
	 */
	@Override
	public Future<CommandResult> executeAsync(final ICommand command, final CommandReturnType commandReturnType) {

		Ensure.notNull(commandResultProcessor, "commandResultProcessor");

		RipenFuture<CommandResult> remoteTaskCompletionSource = new RipenFuture<>();
		commandResultProcessor.regiesterProcessingCommand(command, commandReturnType, remoteTaskCompletionSource);
		
		try {
			await(systemAsyncHelper.submit(() -> {
				return await(sendQueueMessageService.sendMessageAsync(
						producer,
						"command",
						command.getClass().getSimpleName(),
						buildCommandMessage(command, true),
						command.getAggregateRootId(),
						command.getId(),
						command.getItems()));
			}));
		} catch (Exception e) {
			commandResultProcessor.processFailedSendingCommand(command);
			throw e;
		}

		return remoteTaskCompletionSource;
		
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