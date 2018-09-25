package com.jiefzz.ejoker.queue.command;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.rocketmq.client.exception.MQClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.AggregateRootAlreadyExistException;
import com.jiefzz.ejoker.commanding.CommandResult;
import com.jiefzz.ejoker.commanding.CommandReturnType;
import com.jiefzz.ejoker.commanding.CommandRuntimeException;
import com.jiefzz.ejoker.commanding.ICommand;
import com.jiefzz.ejoker.commanding.ICommandExecuteContext;
import com.jiefzz.ejoker.commanding.ICommandProcessor;
import com.jiefzz.ejoker.commanding.ProcessingCommand;
import com.jiefzz.ejoker.domain.IAggregateRoot;
import com.jiefzz.ejoker.domain.IAggregateStorage;
import com.jiefzz.ejoker.domain.IRepository;
import com.jiefzz.ejoker.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.queue.SendReplyService;
import com.jiefzz.ejoker.queue.completation.DefaultMQConsumer;
import com.jiefzz.ejoker.queue.completation.EJokerQueueMessage;
import com.jiefzz.ejoker.queue.completation.IEJokerQueueMessageContext;
import com.jiefzz.ejoker.queue.domainEvent.DomainEventConsumer;
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.service.IWorkerService;
import com.jiefzz.ejoker.z.common.utils.Ensure;

@EService
public class CommandConsumer implements IWorkerService {

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
	
	/// #fix 180920 register sync offset task
	@Dependence
	private IScheduleService scheduleService;
	
	private static long taskIndex = 0;
	
	private final long tx = ++taskIndex;
	///
	
	private DefaultMQConsumer consumer;
	
	private Map<String, Class<? extends ICommand>> commandTypeDict = new HashMap<>();
	
	public DefaultMQConsumer getConsumer() {
		return consumer;
	}
	
	public CommandConsumer useConsumer(DefaultMQConsumer consumer) {
		this.consumer = consumer;
		return this;
	}

	public void handle(EJokerQueueMessage queueMessage, IEJokerQueueMessageContext context) {
		
		// Here QueueMessage is a carrier of Command
		// separate it from  QueueMessage；
		HashMap<String, String> commandItems = new HashMap<>();
		String messageBody = new String(queueMessage.getBody(), Charset.forName("UTF-8"));
		CommandMessage commandMessage = jsonSerializer.revert(messageBody, CommandMessage.class);
		Class<? extends ICommand> commandType = getCommandPrototype(queueMessage.getTag());
		ICommand command = jsonSerializer.revert(commandMessage.commandData, commandType);
		CommandExecuteContext commandExecuteContext = new CommandExecuteContext(
				repository,
				aggregateRootStorage,
				queueMessage,
				context,
				commandMessage,
				sendReplyService);
		commandItems.put("CommandReplyAddress", commandMessage.replyAddress);
		processor.process(new ProcessingCommand(command, commandExecuteContext, commandItems));
	}
	
	public CommandConsumer start(){
		consumer.registerEJokerCallback((eJokerMsg, context) -> handle(eJokerMsg, context));
		try {
			consumer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		/// #fix 180920 register sync offset task
		{
			scheduleService.startTask(this.getClass().getName() + "#sync offset task" + tx, () -> {
				consumer.syncOffsetToBroker();
			}, 2000, 2000);
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

		/// #fix 180920 register sync offset task
		{
			scheduleService.stopTask(DomainEventConsumer.class.getName() + "#sync offset task" + tx);
		}
		///
		
		return this;
	}
	
	private Class<? extends ICommand> getCommandPrototype(String commandTypeString) {
		Ensure.notNullOrEmpty(commandTypeString, commandTypeString);
		Class<? extends ICommand> commandType = commandTypeDict.getOrDefault(commandTypeString, null);
		if(null!=commandType)
			return commandType;
		try {
			commandType = (Class<? extends ICommand> )Class.forName(commandTypeString);
			commandTypeDict.put(commandTypeString, commandType);
			return commandType;
		} catch (ClassNotFoundException e) {
			String format = String.format("Defination of [%s] is not found!!!", commandTypeString);
			logger.error(format);
			throw new CommandRuntimeException(format);
		}
	}

	/**
	 * commandHandler处理过程中，使用的上下文就是这个上下文。<br>
	 * 他能新增一个聚合根，取出聚合跟，修改聚合并提交发布，都在次上下文中提供调用
	 * @author kimffy
	 *
	 */
	class CommandExecuteContext implements ICommandExecuteContext {
		
		private String result;
		
		private final Map<String, IAggregateRoot> trackingAggregateRootDict = new ConcurrentHashMap<>();
		
		private final IRepository repository;
		
		private final IAggregateStorage aggregateRootStorage;
		
		private final SendReplyService sendReplyService;
		
		private final EJokerQueueMessage message;
		
		private final IEJokerQueueMessageContext messageContext;
		
		private final CommandMessage commandMessage;

		public CommandExecuteContext(IRepository repository, IAggregateStorage aggregateRootStorage, EJokerQueueMessage message, IEJokerQueueMessageContext messageContext, CommandMessage commandMessage, SendReplyService sendReplyService) {
			this.repository = repository;
			this.aggregateRootStorage = aggregateRootStorage;
			this.sendReplyService = sendReplyService;
			this.message = message;
			this.commandMessage = commandMessage;
			this.messageContext = messageContext;
		}

		@Override
		public void onCommandExecuted(CommandResult commandResult) {
			messageContext.onMessageHandled(message);

			if (null == commandMessage.replyAddress || "".equals(commandMessage.replyAddress))
				return;
			
			sendReplyService.sendReply(CommandReturnType.CommandExecuted.ordinal(), commandResult, commandMessage.replyAddress);
		}

		@Override
		public void add(IAggregateRoot aggregateRoot) {
			if (aggregateRoot == null)
				throw new ArgumentNullException("aggregateRoot");
			String uniqueId = aggregateRoot.getUniqueId();
			if(null!=trackingAggregateRootDict.putIfAbsent(uniqueId, aggregateRoot))
				throw new AggregateRootAlreadyExistException(uniqueId, aggregateRoot.getClass());
		}

		@Override
		public <T extends IAggregateRoot> T get(Object id, Class<T> clazz, boolean firstFromCache) {
			if (id == null)
				throw new ArgumentNullException("id");

			String aggregateRootId = id.toString();
			IAggregateRoot aggregateRoot = null;
			
			//try get aggregate root from the last execute context.
			if (null!=(aggregateRoot = trackingAggregateRootDict.getOrDefault(aggregateRootId, null)))
				return (T )aggregateRoot;

			if (firstFromCache)
				aggregateRoot = repository.get((Class<IAggregateRoot> )clazz, id);
			else
				aggregateRoot = aggregateRootStorage.get((Class<IAggregateRoot> )clazz, aggregateRootId);

			if (aggregateRoot != null) {
				trackingAggregateRootDict.put(aggregateRoot.getUniqueId(), aggregateRoot);
				return (T )aggregateRoot;
			}

			return null;
		}

		@Override
		public <T extends IAggregateRoot> T get(Object id, Class<T> clazz) {
			return get(id, clazz, true);
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
	}

}
