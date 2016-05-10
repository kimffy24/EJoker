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
import com.jiefzz.ejoker.infrastructure.z.common.ArgumentNullException;
import com.jiefzz.ejoker.infrastructure.z.common.UnimplementException;
import com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers.Consumer;
import com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers.ConsumerSetting;
import com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers.IMessageContext;
import com.jiefzz.ejoker.infrastructure.z.queue.clients.consumers.IMessageHandler;
import com.jiefzz.ejoker.infrastructure.z.queue.protocols.QueueMessage;
import com.jiefzz.ejoker.queue.SendReplyService;

public class CommandConsumer implements IMessageHandler {

	private final static Logger logger = LoggerFactory.getLogger(CommandConsumer.class);

	private final static String defaultCommandConsumerGroup = "CommandConsumerGroup";

	private final SendReplyService sendReplyService = new SendReplyService();

	@Resource
	private Consumer consumer;
	@Resource
	private IJSONConverter jsonSerializer;
	@Resource
	private ICommandProcessor processor;
	@Resource
	private IRepository repository;
	@Resource
	private IAggregateStorage aggregateRootStorage;

	public CommandConsumer(String groupName, ConsumerSetting setting) {
		//consumer = new Consumer(groupName ?? DefaultCommandConsumerGroup, setting ?? new ConsumerSetting
		//{
		//    ConsumeFromWhere = ConsumeFromWhere.FirstOffset
		//});
	}

	public CommandConsumer(String groupName) {
		this(groupName, null);
	}

	public CommandConsumer() {
		this(defaultCommandConsumerGroup);
	}

	public Consumer getConsumer() { return consumer; }

	@Override
	public void Handle(QueueMessage queueMessage, IMessageContext context) {
		// Here QueueMessage is a carrier of Command
		// separate it from  QueueMessage；
		HashMap<String, String> commandItems = new HashMap<String, String>();
		// TODO unfinished. jsonSerializer.revert() can get type from giving message????
		String bodyString = new String(queueMessage.body, Charset.forName("UTF-8"));
		CommandMessage commandMessage = jsonSerializer.revert(bodyString, CommandMessage.class);
		Class commandType;
		try {
			commandType = Class.forName(queueMessage.tag);
		} catch (ClassNotFoundException e) {
			String format = String.format("Defination of [%s] is not found!!!", queueMessage.tag);
			logger.error(format);
			throw new CommandRuntimeException(format);
		}
		ICommand command = jsonSerializer.revert(commandMessage.commandData, commandType);
		CommandExecuteContext commandExecuteContext = new CommandExecuteContext(repository, aggregateRootStorage, queueMessage, context, commandMessage, sendReplyService);
		commandItems.put("CommandReplyAddress", commandMessage.replyAddress);
		processor.process(new ProcessingCommand(command, commandExecuteContext, commandItems));
	}

	class CommandExecuteContext implements ICommandExecuteContext {
		private String result;
		private final ConcurrentHashMap<String, IAggregateRoot> trackingAggregateRootDict = new ConcurrentHashMap<String, IAggregateRoot>();;
		private final IRepository repository;
		private final IAggregateStorage aggregateRootStorage;
		private final SendReplyService sendReplyService;
		private final QueueMessage queueMessage;
		private final IMessageContext messageContext;
		private final CommandMessage commandMessage;

		public CommandExecuteContext(IRepository repository, IAggregateStorage aggregateRootStorage, QueueMessage queueMessage, IMessageContext messageContext, CommandMessage commandMessage, SendReplyService sendReplyService) {
			this.repository = repository;
			this.aggregateRootStorage = aggregateRootStorage;
			this.sendReplyService = sendReplyService;
			this.queueMessage = queueMessage;
			this.commandMessage = commandMessage;
			this.messageContext = messageContext;
		}

		@Override
		public void onCommandExecuted(CommandResult commandResult) {
			messageContext.onMessageHandled(queueMessage);

			if (commandMessage.replyAddress == null || "".equals(commandMessage.replyAddress))
				return;

			// TODO: Unfinished SendReplyService !!!
			throw new UnimplementException(CommandExecuteContext.class.getName()+"onCommandExecuted()");
			//_sendReplyService.SendReply((int)CommandReplyType.CommandExecuted, commandResult, _commandMessage.ReplyAddress);
		}

		@Override
		public void add(IAggregateRoot aggregateRoot) {
			if (aggregateRoot == null)
				throw new ArgumentNullException("aggregateRoot");
			String uniqueId = aggregateRoot.getUniqueId();
			if(trackingAggregateRootDict.containsKey(uniqueId))
				throw new AggregateRootAlreadyExistException(uniqueId, aggregateRoot.getClass());
			trackingAggregateRootDict.put(uniqueId, aggregateRoot);
			//if (!_trackingAggregateRootDict.TryAdd(aggregateRoot.UniqueId, aggregateRoot))
			//{
			//    throw new AggregateRootAlreadyExistException(aggregateRoot.UniqueId, aggregateRoot.GetType());
			//}
		}

		@Override
		public <T extends IAggregateRoot<?>> T get(Object id, boolean firstFromCache) {
			if (id == null)
				throw new ArgumentNullException("id");

			String aggregateRootId = id.toString();
			IAggregateRoot aggregateRoot = null;
			
			// TODO: Perhaps it will has a high-performance method.
			// C# use ConcurrentDictionary.TryGetValue() here
			if (trackingAggregateRootDict.containsKey(aggregateRootId)) {
				return (T) trackingAggregateRootDict.get(aggregateRootId);
			}

			if (firstFromCache) {
				// TODO: This method will throw an UnimplementException now!!!
				aggregateRoot = repository.get(id);
			} else {
				// TODO: This method will get a unexpected result!!!
				aggregateRoot = aggregateRootStorage.get(IAggregateRoot.class, aggregateRootId);
			}

			if (aggregateRoot != null)
			{
				trackingAggregateRootDict.put(aggregateRoot.getUniqueId(), aggregateRoot);
				return (T) aggregateRoot;
			}

			return null;
		}

		@Override
		public <T extends IAggregateRoot<?>> T get(Object id) {
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
