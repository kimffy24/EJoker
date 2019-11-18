package pro.jiefzz.ejoker.bootstrap;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.EJoker;
import pro.jiefzz.ejoker.EJoker.EJokerSingletonFactory;
import pro.jiefzz.ejoker.commanding.ICommand;
import pro.jiefzz.ejoker.common.context.dev2.IEJokerSimpleContext;
import pro.jiefzz.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.common.service.Scavenger;
import pro.jiefzz.ejoker.common.system.enhance.ForEachUtil;
import pro.jiefzz.ejoker.common.system.functional.IFunction;
import pro.jiefzz.ejoker.common.system.functional.IFunction2;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.domain.domainException.IDomainException;
import pro.jiefzz.ejoker.eventing.IDomainEvent;
import pro.jiefzz.ejoker.messaging.IApplicationMessage;
import pro.jiefzz.ejoker.queue.ITopicProvider;
import pro.jiefzz.ejoker.queue.applicationMessage.ApplicationMessageConsumer;
import pro.jiefzz.ejoker.queue.applicationMessage.ApplicationMessagePublisher;
import pro.jiefzz.ejoker.queue.command.CommandConsumer;
import pro.jiefzz.ejoker.queue.command.CommandResultProcessor;
import pro.jiefzz.ejoker.queue.command.CommandService;
import pro.jiefzz.ejoker.queue.domainEvent.DomainEventConsumer;
import pro.jiefzz.ejoker.queue.domainEvent.DomainEventPublisher;
import pro.jiefzz.ejoker.queue.domainException.DomainExceptionConsumer;
import pro.jiefzz.ejoker.queue.domainException.DomainExceptionPublisher;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueConsumer;
import pro.jiefzz.ejoker.queue.skeleton.AbstractEJokerQueueProducer;
import pro.jiefzz.ejoker.queue.skeleton.aware.IConsumerWrokerAware;
import pro.jiefzz.ejoker.queue.skeleton.aware.IProducerWrokerAware;

public class EJokerBootstrap {
	
	private final static  Logger logger = LoggerFactory.getLogger(EJokerBootstrap.class);

	private String eJokerDomainEventGroup = "EjokerDomainEventGroup";
	
	private String eJokerCommandGroup = "EjokerCommandGroup";

	private String eJokerApplicationMessageGroup = "EjokerApplicationMessageGroup";

	private String eJokerDomainExceptionGroup = "EjokerDomainExceptionGroup";

	private IFunction2<IConsumerWrokerAware, String, IEJokerSimpleContext> consumerInstanceCreator = null;
	
	private IFunction2<IProducerWrokerAware, String, IEJokerSimpleContext> producerInstanceCreator = null;
	
	private IVoidFunction1<IEJokerSimpleContext> preInitAll = null;

	private IVoidFunction1<IEJokerSimpleContext> postInitAll = null;
	
	private AtomicBoolean initFlag = new AtomicBoolean(false);
	
	protected final EJokerSingletonFactory eJokerHolder;
	
	protected final IEJokerSimpleContext eJokerContext;
	
	protected final Map<String, Object> cTables2 = new ConcurrentHashMap<>();
	
	public EJokerBootstrap(String... packages) {
		this(() -> EJoker.class, packages);
	}
	
	protected EJokerBootstrap(IFunction<Class<? extends EJoker>> eJokerTypeProvider, String... packages) {
		logger.info("====================== EJokerFramework ======================");
		logger.info("eJoker context initializing ... ");
		
		eJokerHolder = new EJokerSingletonFactory(eJokerTypeProvider.trigger());
		eJokerContext = eJokerHolder.getInstance().getEJokerContext();
		
		{
			IEjokerContextDev2 eJokerFullContext = (IEjokerContextDev2 )eJokerContext;
			ForEachUtil.processForEach(packages, eJokerFullContext::scanPackage);
			eJokerFullContext.refresh();
		}
	}

	public final void discard() {
		IEjokerContextDev2 eJokerFullContext = (IEjokerContextDev2 )eJokerContext;
		eJokerFullContext.discard();
	}
	
	public final void initAll() {
		
		if(!initFlag.compareAndSet(false, true))
			return;
		
		if(null == consumerInstanceCreator || null == producerInstanceCreator)
			throw new RuntimeException("one or both of consumerInstanceCreator and producerInstanceCreator is not set before the invoking of initAll()!!!");
		
		if(null != preInitAll)
			preInitAll.trigger(eJokerContext);
		
		this.initCommandResultProcessor();
		
		this.initCommandService();
		
		this.initPublisher(DomainEventPublisher.class);
		this.initPublisher(ApplicationMessagePublisher.class);
		this.initPublisher(DomainExceptionPublisher.class);
		
		this.initConsumer(DomainExceptionConsumer.class);
		this.initConsumer(ApplicationMessageConsumer.class);
		this.initConsumer(DomainEventConsumer.class);
		this.initConsumer(CommandConsumer.class);
		
		if(null != postInitAll)
			postInitAll.trigger(eJokerContext);
	}
	
	/* ========================= */ // private method
	
	private CommandResultProcessor initCommandResultProcessor() {

		// 启动命令跟踪反馈控制对象
		CommandResultProcessor commandResultProcessor = eJokerContext.get(CommandResultProcessor.class);
		if(null == cTables2.putIfAbsent(CommandResultProcessor.class.getName(), 1)) {
			commandResultProcessor.start();
			//哪里开的就哪里关闭。
			eJokerContext.get(Scavenger.class).addFianllyJob(commandResultProcessor::shutdown);
		}
		return commandResultProcessor;
		
	}
	
	private CommandService initCommandService() {
		// 启动命令服务
		CommandService commandService = eJokerContext.get(CommandService.class);
		if(null == cTables2.putIfAbsent(CommandService.class.getName(), 1)) {
			commandService
				.useProducer(this.producerInstanceCreator.trigger(eJokerCommandGroup, eJokerContext))
				.start();
			//哪里开的就哪里关闭。
			eJokerContext.get(Scavenger.class).addFianllyJob(commandService::shutdown);
		}
		return commandService;
	}

	private <T extends AbstractEJokerQueueConsumer> T initConsumer(Class<T> consumerType) {
		
		Type messageType;
		ITopicProvider<?> topicProvider;
		String groupName;
		if(DomainExceptionConsumer.class.equals(consumerType)) {
			messageType = IDomainException.class;
			groupName = this.eJokerDomainExceptionGroup;
		} else if(ApplicationMessageConsumer.class.equals(consumerType)) {
			messageType = IApplicationMessage.class;
			groupName = this.eJokerApplicationMessageGroup;
		} else if(DomainEventConsumer.class.equals(consumerType)) {
			messageType = IDomainEvent.class;
			groupName = this.eJokerDomainEventGroup;
		} else if(CommandConsumer.class.equals(consumerType)) {
			messageType = ICommand.class;
			groupName = this.eJokerCommandGroup;
		} else {
			throw new RuntimeException(String.format("Unknow process messageTypefor consumer[type=%s]", consumerType.toString()));
		}

		topicProvider = eJokerContext.get(ITopicProvider.class, messageType);

		T consumer = eJokerContext.get(consumerType);
		if(null == cTables2.putIfAbsent(consumerType.getName(), 1)) {
			consumer.useConsumer(this.consumerInstanceCreator.trigger(groupName, eJokerContext));
			Set<String> getAllTopics = topicProvider.GetAllTopics();
			getAllTopics.forEach(s -> {
				try {
					consumer.subscribe(s);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			consumer.start();
			//哪里开的就哪里关闭。
			eJokerContext.get(Scavenger.class).addFianllyJob(consumer::shutdown);
		}
		
		return consumer;
	}

	private <T extends AbstractEJokerQueueProducer<?>> T initPublisher(Class<T> producerType) {
		
//		Class<?> messageType;
		String groupName;
		if(DomainExceptionPublisher.class.equals(producerType)) {
//			messageType = IDomainException.class;
			groupName = this.eJokerDomainExceptionGroup;
		} else if(ApplicationMessagePublisher.class.equals(producerType)) {
//			messageType = IApplicationMessage.class;
			groupName = this.eJokerApplicationMessageGroup;
		} else if(DomainEventPublisher.class.equals(producerType)) {
//			messageType = IDomainEvent.class;
			groupName = this.eJokerDomainEventGroup;
		} else {
			throw new RuntimeException(String.format("Unknow process messageTypefor consumer[type=%s]", producerType.toString()));
		}
		// CommandProducer 的职责由 CommandService 行使。 CommandService 与其他3个消息的逻辑不一致，所以单独初始化

		T producer = eJokerContext.get(producerType/*, messageType*/);
		if(null == cTables2.putIfAbsent(producerType.getName(), 1)) {
			producer.useProducer(this.producerInstanceCreator.trigger(groupName, eJokerContext));
			producer.start();
			//哪里开的就哪里关闭。
			eJokerContext.get(Scavenger.class).addFianllyJob(producer::shutdown);
		}
		
		return producer;
	}
	
	/* ========================= */ //getter and setter
	
	public final IEJokerSimpleContext getEJokerContext() {
		return eJokerContext;
	}
	
	public final EJokerBootstrap setEJokerDomainEventGroup(String eJokerDomainEventGroup) {
		this.eJokerDomainEventGroup = eJokerDomainEventGroup;
		return this;
	}

	public final EJokerBootstrap setEJokerCommandGroup(String eJokerCommandGroup) {
		this.eJokerCommandGroup = eJokerCommandGroup;
		return this;
	}

	public final EJokerBootstrap setEJokerApplicationMessageGroup(String eJokerApplicationMessageGroup) {
		this.eJokerApplicationMessageGroup = eJokerApplicationMessageGroup;
		return this;
	}

	public final EJokerBootstrap setEJokerDomainExceptionGroup(String eJokerDomainExceptionGroup) {
		this.eJokerDomainExceptionGroup = eJokerDomainExceptionGroup;
		return this;
	}
	
	public final EJokerBootstrap setConsumerInstanceCreator(
			IFunction2<IConsumerWrokerAware, String, IEJokerSimpleContext> consumerInstanceCreator) {
		this.consumerInstanceCreator = consumerInstanceCreator;
		return this;
	}

	public final EJokerBootstrap setProducerInstanceCreator(
			IFunction2<IProducerWrokerAware, String, IEJokerSimpleContext> producerInstanceCreator) {
		this.producerInstanceCreator = producerInstanceCreator;
		return this;
	}

	public final EJokerBootstrap setPreInitAll(IVoidFunction1<IEJokerSimpleContext> preInitAll) {
		this.preInitAll = preInitAll;
		return this;
	}

	public final EJokerBootstrap setPostInitAll(IVoidFunction1<IEJokerSimpleContext> postInitAll) {
		this.postInitAll = postInitAll;
		return this;
	}
	
}
