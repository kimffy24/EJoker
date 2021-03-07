package pro.jk.ejoker.queue.command;

import static pro.jk.ejoker.common.system.extension.LangUtil.await;

import java.nio.charset.Charset;
import java.util.concurrent.Future;

import pro.jk.ejoker.commanding.CommandResult;
import pro.jk.ejoker.commanding.CommandReturnType;
import pro.jk.ejoker.commanding.ICommand;
import pro.jk.ejoker.commanding.ICommandService;
import pro.jk.ejoker.common.context.annotation.context.Dependence;
import pro.jk.ejoker.common.context.annotation.context.EService;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.common.service.IWorkerService;
import pro.jk.ejoker.common.system.extension.acrossSupport.RipenFuture;
import pro.jk.ejoker.common.system.helper.Ensure;
import pro.jk.ejoker.common.system.task.context.SystemAsyncHelper;
import pro.jk.ejoker.infrastructure.ITypeNameProvider;
import pro.jk.ejoker.queue.ITopicProvider;
import pro.jk.ejoker.queue.QueueMessageTypeCode;
import pro.jk.ejoker.queue.SendQueueMessageService;
import pro.jk.ejoker.queue.SendQueueMessageService.SendServiceContext;
import pro.jk.ejoker.queue.skeleton.aware.EJokerQueueMessage;
import pro.jk.ejoker.queue.skeleton.aware.IProducerWrokerAware;

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
	private IJSONStringConverterPro jsonConverter;
	
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
				buildCommandMessage(command));
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
						buildCommandMessage(command, true)));
			}));
		} catch (Exception e) {
			commandResultProcessor.processFailedSendingCommand(command);
			throw e;
		}

		return remoteTaskCompletionSource;
		
	}

	private SendServiceContext buildCommandMessage(ICommand command){
		return buildCommandMessage(command, false);
	}

	private SendServiceContext buildCommandMessage(ICommand command, boolean needReply){
		Ensure.notNull(command.getAggregateRootId(), "aggregateRootId");
        String commandData = jsonConverter.convert(command);
        String topic = commandTopicProvider.getTopic(command);
        String replyAddress = needReply && (null!=commandResultProcessor) ? commandResultProcessor.getBindingAddress() : null;
        String messageData = jsonConverter.convert(new CommandMessage(commandData, replyAddress));
        String tag = typeNameProvider.getTypeName(command.getClass());

		return new SendServiceContext(
				"command",
				command.getClass().getSimpleName(),
				new EJokerQueueMessage(
		            topic,
		            QueueMessageTypeCode.CommandMessage.ordinal(),
		            messageData.getBytes(Charset.forName("UTF-8")),
		            tag),
				messageData,
				command.getAggregateRootId(),
				command.getId(),
				command.getItems());
	}
}