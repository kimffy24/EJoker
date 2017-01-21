package com.jiefzz.ejoker.queue.command;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.commanding.AggregateRootAlreadyExistException;
import com.jiefzz.ejoker.commanding.CommandResult;
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
import com.jiefzz.ejoker.z.common.ArgumentNullException;
import com.jiefzz.ejoker.z.common.UnimplementException;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.queue.IConsumer;
import com.jiefzz.ejoker.z.queue.IQueueWokerService;
import com.jiefzz.ejoker.z.queue.clients.consumers.ConsumerSetting;
import com.jiefzz.ejoker.z.queue.clients.consumers.IMessageContext;
import com.jiefzz.ejoker.z.queue.clients.consumers.IMessageHandler;
import com.jiefzz.ejoker.z.queue.protocols.Message;

@EService
public class CommandConsumer implements IQueueWokerService,IMessageHandler {

	final static Logger logger = LoggerFactory.getLogger(CommandConsumer.class);

	private final static String defaultCommandConsumerGroup = "CommandConsumerGroup";

	@Resource
	private SendReplyService sendReplyService;
	@Resource
	private IJSONConverter jsonSerializer;
	@Resource
	private ICommandProcessor processor;
	@Resource
	private IRepository repository;
	@Resource
	private IAggregateStorage aggregateRootStorage;

	private IConsumer consumer;
	
	public CommandConsumer(String groupName, ConsumerSetting setting) {}

	public CommandConsumer(String groupName) { this(groupName, null); }

	public CommandConsumer() { this(defaultCommandConsumerGroup); }

	public IConsumer getConsumer() { return consumer; }
	public CommandConsumer useConsumer(IConsumer consumer) { this.consumer = consumer; return this;}

	@Override
	public void handle(Message message, IMessageContext context) {
		
		// Here QueueMessage is a carrier of Command
		// separate it from  QueueMessage；
		HashMap<String, String> commandItems = new HashMap<String, String>();
		String messageBody = new String(message.body, Charset.forName("UTF-8"));
		CommandMessage commandMessage = jsonSerializer.revert(messageBody, CommandMessage.class);
		Class<? extends ICommand> commandType;
		try {
			commandType = (Class<? extends ICommand> )Class.forName(message.tag);
		} catch (ClassNotFoundException e) {
			String format = String.format("Defination of [%s] is not found!!!", message.tag);
			logger.error(format);
			throw new CommandRuntimeException(format);
		}
		ICommand command = jsonSerializer.revert(commandMessage.commandData, commandType);
		CommandExecuteContext commandExecuteContext = new CommandExecuteContext(repository, aggregateRootStorage, message, context, commandMessage, sendReplyService);
		commandItems.put("CommandReplyAddress", commandMessage.replyAddress);
		processor.process(new ProcessingCommand(command, commandExecuteContext, commandItems));
	}
	
	@Override
	public IQueueWokerService start() {
		consumer.setMessageHandler(this).start();
		return this;
	}

	@Override
	public IQueueWokerService subscribe(String topic) {
		consumer.subscribe(topic);
		return this;
	}

	@Override
	public IQueueWokerService shutdown() {
		consumer.shutdown();
		return this;
	}

	/**
	 * commandHandler处理过程中，使用的上下文就是这个上下文。<br>
	 * 他能新增一个聚合根，取出聚合跟，修改聚合并提交发布，都在次上下文中提供调用
	 * @author kimffy
	 *
	 */
	class CommandExecuteContext implements ICommandExecuteContext {
		private String result;
		private final ConcurrentHashMap<String, IAggregateRoot> trackingAggregateRootDict = new ConcurrentHashMap<String, IAggregateRoot>();;
		private final IRepository repository;
		private final IAggregateStorage aggregateRootStorage;
		private final SendReplyService sendReplyService;
		//private final QueueMessage queueMessage; // in eNode Message is the actually type of QueueMessage
		private final Message message;
		private final IMessageContext messageContext;
		private final CommandMessage commandMessage;

		public CommandExecuteContext(IRepository repository, IAggregateStorage aggregateRootStorage, Message message, IMessageContext messageContext, CommandMessage commandMessage, SendReplyService sendReplyService) {
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

			if (commandMessage.replyAddress == null || "".equals(commandMessage.replyAddress))
				return;

			// TODO: Unfinished SendReplyService !!!
			throw new UnimplementException(CommandExecuteContext.class.getName()+"#onCommandExecuted()");
			//_sendReplyService.SendReply((int)CommandReplyType.CommandExecuted, commandResult, _commandMessage.ReplyAddress);
		}

		@Override
		public void add(IAggregateRoot aggregateRoot) {
			if (aggregateRoot == null)
				throw new ArgumentNullException("aggregateRoot");
			String uniqueId = aggregateRoot.getUniqueId();
			// TODO: Please update this region with high-performance method.
			if(trackingAggregateRootDict.containsKey(uniqueId))
				throw new AggregateRootAlreadyExistException(uniqueId, aggregateRoot.getClass());
			trackingAggregateRootDict.put(uniqueId, aggregateRoot);
			//if (!_trackingAggregateRootDict.TryAdd(aggregateRoot.UniqueId, aggregateRoot))
			//{
			//    throw new AggregateRootAlreadyExistException(aggregateRoot.UniqueId, aggregateRoot.GetType());
			//}
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
		
		/**
		 * @deprecated
		 */
		@Override
		public <T extends IAggregateRoot> T get(Object id, boolean firstFromCache) {
			throw new CommandRuntimeException("Do not use this method!!! Please use get(Object, Class, boolean) or get(Object, Class)");
//			CommandConsumer.logger.warn("com.jiefzz.ejoker.queue.command.CommandConsumer.CommandExecuteContext.get(Object, boolean) maybe work down with unexcpected error!!!");
//			
//			if (id == null)
//				throw new ArgumentNullException("id");
//
//			String aggregateRootId = id.toString();
//			IAggregateRoot aggregateRoot = null;
//			
//			// TODO: Perhaps it will has a high-performance method.
//			// C# use ConcurrentDictionary.TryGetValue() here
//			if (trackingAggregateRootDict.containsKey(aggregateRootId))
//				return (T) trackingAggregateRootDict.get(aggregateRootId);
//
//			if (firstFromCache) {
//				// TODO: This method will throw an UnimplementException now!!!
//				aggregateRoot = repository.get(id);
//			} else {
//				// TODO: This method will get a unexpected result!!!
//				aggregateRoot = aggregateRootStorage.get(IAggregateRoot.class, aggregateRootId);
//			}
//
//			if (aggregateRoot != null) {
//				trackingAggregateRootDict.put(aggregateRoot.getUniqueId(), aggregateRoot);
//				return (T) aggregateRoot;
//			}
//
//			return null;
		}

		/**
		 * @deprecated
		 */
		@Override
		public <T extends IAggregateRoot> T get(Object id) {
			return get(id, true);
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
