package com.jiefzz.ejoker.queue.command;

import static com.jiefzz.ejoker.z.common.system.extension.acrossSupport.LangUtil.await;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.AggregateRootAlreadyExistException;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandExecuteContext;
import com.jiefzz.ejoker.commanding.ICommandProcessor;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IRepository;
import com.jiefzz.ejoker.infrastructure.ITypeNameProvider;
import com.jiefzz.ejoker.queue.SendReplyService;
import com.jiefzz.ejoker.queue.completation.DefaultMQConsumer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.queue.completation.IEJokerQueueMessageContext;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.service.IJSONConverter;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapperUtil;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.task.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;
import com.jiefzz.ejoker.z.common.task.context.SystemAsyncHelper;

@EService
public class CommandConsumer implements IWorkerService {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(CommandConsumer.class);

	@Dependence
	private SendReplyService sendReplyService;

	@Dependence
	private IJSONConverter jsonSerializer;

	@Dependence
	private ICommandProcessor processor;

	@Dependence
	private IRepository repository;

	@Dependence
	private IAggregateStorage aggregateRootStorage;

	@Dependence
	private ITypeNameProvider typeNameProvider;

	@Dependence
	private SystemAsyncHelper systemAsyncHelper;

	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;

	/// #fix 180920 register sync offset task
	@Dependence
	private IScheduleService scheduleService;

	///

	private DefaultMQConsumer consumer;

	public DefaultMQConsumer getConsumer() {
		return consumer;
	}

	public CommandConsumer useConsumer(DefaultMQConsumer consumer) {
		this.consumer = consumer;
		return this;
	}
	
	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		// Here QueueMessage is a carrier of Command
		// separate it from QueueMessage；
		HashMap<String, String> commandItems = new HashMap<>();
		String messageBody = new String(queueMessage.getBody(), Charset.forName("UTF-8"));
		CommandMessage commandMessage = jsonSerializer.revert(messageBody, CommandMessage.class);
		Class<? extends ICommand> commandType = (Class<? extends ICommand>) typeNameProvider
				.getType(queueMessage.getTag());
		ICommand command = jsonSerializer.revert(commandMessage.commandData, commandType);
		CommandExecuteContext commandExecuteContext = new CommandExecuteContext(queueMessage, context, commandMessage);
		commandItems.put("CommandReplyAddress", commandMessage.replyAddress);
		processor.process(new ProcessingCommand(command, commandExecuteContext, commandItems));
	}

	public CommandConsumer start() {
		consumer.registerEJokerCallback(this::handle);
		try {
			consumer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		/// #fix 180920 register sync offset task
		{
			scheduleService.startTask(this.getClass().getName() + "@" + this.hashCode() + "#sync offset task",
					consumer::syncOffsetToBroker, 2000, 2000);
		}
		///

		return this;
	}

	public CommandConsumer subscribe(String topic) throws Exception {
		consumer.subscribe(topic, "*");
		return this;
	}

	public CommandConsumer shutdown() {
		consumer.shutdown();

		return this;
	}

	/**
	 * commandHandler处理过程中，使用的上下文就是这个上下文。<br>
	 * 他能新增一个聚合根，取出聚合跟，修改聚合并提交发布，都在次上下文中提供调用
	 * 
	 * @author kimffy
	 *
	 */
	class CommandExecuteContext implements ICommandExecuteContext {

		private String result;

		private final Map<String, IAggregateRoot> trackingAggregateRootDict = new HashMap<>();

		private final EJokerQueueMessage message;

		private final IEJokerQueueMessageContext messageContext;

		private final CommandMessage commandMessage;

		public CommandExecuteContext(EJokerQueueMessage message, IEJokerQueueMessageContext messageContext,
				CommandMessage commandMessage) {
			this.message = message;
			this.commandMessage = commandMessage;
			this.messageContext = messageContext;
		}

		@Override
		public SystemFutureWrapper<Void> onCommandExecutedAsync(CommandResult commandResult) {

			messageContext.onMessageHandled(message);

			if (null == commandMessage.replyAddress || "".equals(commandMessage.replyAddress))
				return SystemFutureWrapperUtil.createCompleteFuture();

			return sendReplyService.sendReply(CommandReturnType.CommandExecuted.ordinal(), commandResult,
					commandMessage.replyAddress);

		}

		@Override
		public void add(IAggregateRoot aggregateRoot) {
			if (aggregateRoot == null)
				throw new ArgumentNullException("aggregateRoot");
			String uniqueId = aggregateRoot.getUniqueId();
			if (null != trackingAggregateRootDict.putIfAbsent(uniqueId, aggregateRoot))
				throw new AggregateRootAlreadyExistException(uniqueId, aggregateRoot.getClass());
		}

		@Override
		public SystemFutureWrapper<AsyncTaskResult<Void>> addAsync(IAggregateRoot aggregateRoot) {
			return eJokerAsyncHelper.submit(() -> add(aggregateRoot));
		}

		@Override
		public <T extends IAggregateRoot> SystemFutureWrapper<T> getAsync(Object id, Class<T> clazz,
				boolean tryFromCache) {

			RipenFuture<T> ripenFuture = new RipenFuture<>();
			if (id == null) {
				ripenFuture.trySetException(new ArgumentNullException("id"));
				return new SystemFutureWrapper<>(ripenFuture);
			}
			
			return systemAsyncHelper.submit(() -> get(id, clazz, tryFromCache));
		}

		@Override
		public Collection<IAggregateRoot> getTrackedAggregateRoots() {
			return trackingAggregateRootDict.values();
		}

		@Override
		public void clear() {
			trackingAggregateRootDict.clear();
			result = null;
		}

		@Override
		public void setResult(String result) {
			this.result = result;
		}

		@Override
		public String getResult() {
			return result;
		}

		private <T extends IAggregateRoot> T get(Object id, Class<T> clazz, boolean tryFromCache) {

			RipenFuture<T> ripenFuture = new RipenFuture<>();
			if (id == null) {
				throw new ArgumentNullException("id");
			}

			String aggregateRootId = id.toString();
			IAggregateRoot aggregateRoot = null;

			// try get aggregate root from the last execute context.
			if (null != (aggregateRoot = trackingAggregateRootDict.get(aggregateRootId)))
				return (T) aggregateRoot;

			if (tryFromCache)
				// TODO @await
				aggregateRoot = await(repository.getAsync((Class<IAggregateRoot>) clazz, id));
			else
				// TODO @await
				aggregateRoot = await(aggregateRootStorage.getAsync((Class<IAggregateRoot>) clazz, aggregateRootId));
			if (aggregateRoot != null) {
				trackingAggregateRootDict.put(aggregateRoot.getUniqueId(), aggregateRoot);
				return (T) aggregateRoot;
			}

			return null;
		}

		@Override
		public void onCommandExecuted(CommandResult commandResult) {

			messageContext.onMessageHandled(message);

			if (null == commandMessage.replyAddress || "".equals(commandMessage.replyAddress))
				return;

			sendReplyService
					.sendReply(CommandReturnType.CommandExecuted.ordinal(), commandResult, commandMessage.replyAddress)
					.get();

		}

	}

}
