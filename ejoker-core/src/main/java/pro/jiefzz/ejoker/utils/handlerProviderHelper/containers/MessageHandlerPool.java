package pro.jiefzz.ejoker.utils.handlerProviderHelper.containers;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.jiefzz.ejoker.common.context.dev2.IEjokerContextDev2;
import pro.jiefzz.ejoker.common.system.enhance.EachUtilx;
import pro.jiefzz.ejoker.common.system.enhance.MapUtil;
import pro.jiefzz.ejoker.common.system.enhance.StringUtilx;
import pro.jiefzz.ejoker.common.system.extension.AsyncWrapperException;
import pro.jiefzz.ejoker.common.system.functional.IFunction;
import pro.jiefzz.ejoker.common.system.functional.IFunction1;
import pro.jiefzz.ejoker.common.system.task.AsyncTaskResult;
import pro.jiefzz.ejoker.infrastructure.impl.AbstractMessageHandler;
import pro.jiefzz.ejoker.messaging.IMessage;
import pro.jiefzz.ejoker.messaging.IMessageHandler;
import pro.jiefzz.ejoker.messaging.IMessageHandlerProxy;

/**
 * 由于message类型可以有多个handler，<br />
 * 所以用起来会有点复杂。<br /><br />
 * 
 * 分单个Message和多个Message对象，虽然语法上多个Message能兼容单个message但使用时单个Message属于绝大多数情况，值得特别对待。<br /><br /><br /><br />
 * 
 * * 思路： 把所有定义的handler的参数表，按照 签名信息 -> 代理对象 的键值对在某个字典中存起来先 （在框架构建扫描阶段完成）<br />
 * *       签名信息用参数表的对象的全限定名字按ascii排序后字符串连接得到，同时获得ascii序和调用序的映射，以方便后续调用 <br />
 * *       进入工作阶段(需要接受消息并处理的阶段)，先把调用的消息的参数表按对象的全限定名字按ascii排序成一个全集， <br />
 * *       并计算起每一个子集合的拼接签名信息，从上一个键值对存储的字典中一个一个查找，并把查找到的结果缓存起来，以便后续使用 <br />
 * *       格式满足  全限定名字ascii序签名信息 -&gt; 位图标识 -&gt; handlerProxy列表 这样的3级格式。 <br />
 * *		<br />
 * *		位图标识 用来指示在 ascii序参数标识的位置情况<br />
 * *		<br />
 * *		例如 { DE1, DE2, DE3 } 这样一个满足ascii序的参数表， 6 (110) 表示的子集合是 {DE1, DE2} <br />
 * *		4 (100) 表示的子集合是 {DE1}， 以此类推
 * @author kimffy
 *
 */
public class MessageHandlerPool {
	
	private final static Logger logger = LoggerFactory.getLogger(MessageHandlerPool.class);

	public final static String HANDLER_METHOD_NAME = "handleAsync";

	private final static Class<?> PARAMETER_TYPE_CONTRAINT = IMessage.class;

	private final Map<String, List<MessageHandlerReflectionTuple>> handlerFullContraintMapper = new HashMap<>();

	private final Map<String, Map<Long, List<MessageHandlerReflectionTuple>>> handlerMoreThanOneLocater = new ConcurrentHashMap<>();
	
	public final void regist(Class<? extends IMessageHandler> implementationHandlerClazz, IFunction<IEjokerContextDev2> ejokerProvider) {
		
		String actuallyHandlerName = implementationHandlerClazz.getName();
		for(Class<?> clazz = implementationHandlerClazz;
				!Object.class.equals(clazz) && !AbstractMessageHandler.class.equals(clazz) && null != clazz;
				clazz = clazz.getSuperclass() ) {
			final Method[] declaredMethods = clazz.getDeclaredMethods();
			for (int i = 0; i < declaredMethods.length; i++) {
				Method method = declaredMethods[i];
				
				// 仅处理名字为HANDLER_METHOD_NAME的方法，这里是handleAsync
				if (!HANDLER_METHOD_NAME.equals(method.getName()))
					continue;
				
				// 判断所有的参数的类型均为PARAMETER_TYPE_CONTRAINT的子类，这里是IMessage
				Class<?>[] parameterTypes = method.getParameterTypes();
				if(null == parameterTypes || 0 == parameterTypes.length)
					throw new RuntimeException(String.format("Parameter signature of %s#%s() is not accept!!!", actuallyHandlerName, method.getName()));
				boolean pOk = true;
				String pList = "";
				for(Class<?> pClazz : parameterTypes) {
					if(!PARAMETER_TYPE_CONTRAINT.isAssignableFrom(pClazz) || PARAMETER_TYPE_CONTRAINT.equals(pClazz)) {
						pOk = false;
					}
					pList += pClazz.getSimpleName();
					pList += ", ";
				}
				pList = pList.substring(0, pList.lastIndexOf(','));
				if(!pOk)
					throw new RuntimeException(String.format("Parameter signature of %s#%s(%s) is not accept!!!", actuallyHandlerName, method.getName(), pList));
				
				{
					// 约束返回类型。 java无法在编译时约束，那就推到运行时上约束吧
					// 这里就是检查返回类型(带泛型)为 Future<AsyncTaskResult<Void>>
					boolean isOK = false;
					Type genericReturnType = method.getGenericReturnType();
					if(genericReturnType instanceof ParameterizedType) {
						ParameterizedType parameterizedType = (ParameterizedType )genericReturnType;
						if(parameterizedType.getRawType().equals(Future.class)) {
							Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
							if(null != actualTypeArguments && 1 == actualTypeArguments.length) {
								Type type = actualTypeArguments[0];
								if(type instanceof ParameterizedType) {
									ParameterizedType parameterizedTypeInnerLv1 = (ParameterizedType )type;
									if(parameterizedTypeInnerLv1.getRawType().equals(AsyncTaskResult.class)) {
										Type[] actualTypeArgumentsLv1 = parameterizedTypeInnerLv1.getActualTypeArguments();
										if(null != actualTypeArgumentsLv1 && 1 == actualTypeArgumentsLv1.length) {
											Type typeLv2 = actualTypeArgumentsLv1[0];
											if(Void.class.equals(typeLv2)) {
												isOK = true;
											}
										}
									}
								}
							}
						}
					}
					if(!isOK) {
						String errorDesc = String.format("The method which Proxy will point to should return Future<AsyncTaskResult<Void>> !!! [currentMethod: {}#{}]", actuallyHandlerName, HANDLER_METHOD_NAME);
						logger.error(errorDesc);
						throw new RuntimeException(errorDesc);
					}
				}

				// 设置方法的访问权限
				if (!method.isAccessible())
					method.setAccessible(true);
				
				// 获取参数表全限定名称ascii序的拼接字符串pS （parametersSignature）
				Set<String> pSet = getStringDescOrderly(m -> m.getName(), parameterTypes);
				String pS = getFullPs(pSet);

				// 按拼接字符串pS注册到一个hashMap中供后面使用
				List<MessageHandlerReflectionTuple> handlerInvokerList = getProxyAsyncHandlers(pS);
				MessageHandlerReflectionTuple reflectionTuple;
				if(1 == parameterTypes.length) {
					reflectionTuple = new MessageHandlerReflectionTuple(method, pList, ejokerProvider);
				} else {
					// 多于1个的参数时，需要把ascii序和invoke调用时参数表的顺序的位置对应信息记录下来，
					// 以便调用时对应函数签名的顺序
					// O(n) = n^2 但是可以接受，考虑到这里是一次性构建 以及 可预期的不会出现超级多参数的handler
					List<Integer> pOrder = new ArrayList<>();
					for(String p : pSet) {
						for(int index = 0; index<parameterTypes.length; index++) {
							if(p.equals(parameterTypes[index].getName())) {
								pOrder.add(index);
								break;
							}
						}
					}
					reflectionTuple = new MessageHandlerReflectionTuple(method, pList, pOrder, ejokerProvider);
				}
				handlerInvokerList.add(reflectionTuple);
					
			}
		}
		
	}
	
	public final List<MessageHandlerReflectionTuple> getProxyAsyncHandlers(String messageTypeName) {
		return MapUtil.getOrAdd(handlerFullContraintMapper, messageTypeName, () -> new ArrayList<>());
	}

	public final List<MessageHandlerReflectionTuple> getProxyAsyncHandlers(Class<? extends IMessage> messageType) {
		return getProxyAsyncHandlers(messageType.getName());
	}
	
	public final List<MessageHandlerReflectionTuple> getProxyAsyncHandlers(IMessage messageType) {
		return getProxyAsyncHandlers(messageType.getClass());
	}
	
	/**
	 * 由于拿着位图信息和messageHandler的代理对象去工作还是要做很多额外的流程，<br />
	 * 才能正确调用到messageHandler，所以把大部分流程封装起来，主要有<br /><br />
	 * 
	 * 1. 按ascii给参数表排序。
	 * 2. 找出消息列表以及它的子集所有可调用的handler（如果有的话）<br />
	 * 3.1. 对应多参数的情况，按照调用序重组参数表<br />
	 * 3.2. 反射执行handler方法<br />
	 * 
	 * @param messages
	 */
	public final void processMultiMessages(IMessage... messages) {

		String orderlyPs;
		IMessage[] orderlyMsgs;
		
		// 现将入参按照类型的全限定名称ascii顺序排好
		if(1 == messages.length) {
			orderlyPs = messages[0].getClass().getName();
			orderlyMsgs = messages;
		} else {
			orderlyMsgs = new IMessage[messages.length];
			StringBuilder orderlyPsSb = new StringBuilder();
			AtomicInteger ai = new AtomicInteger(0);
			Map<String, IMessage> orderlyArgs = new TreeMap<>((s1, s2) -> s1.compareTo(s2));
			for(IMessage msg : messages) {
				orderlyArgs.put(msg.getClass().getName(), msg);
			}
			EachUtilx.forEach(orderlyArgs, (k, v) -> {
				orderlyPsSb.append(k);
				orderlyMsgs[ai.getAndIncrement()] = v;
			});
			orderlyPs = orderlyPsSb.toString();
		}
		
		Map<Long, List<MessageHandlerReflectionTuple>> proxyAsyncMultiHandlers = getProxyAsyncMultiHandlers(orderlyPs, orderlyMsgs);
		EachUtilx.forEach(proxyAsyncMultiHandlers, (b, l) -> {
			
			for(MessageHandlerReflectionTuple reflectionTuple : l) {
				IMessage[] invokeList = contructPTable(b, reflectionTuple.asciiOrder, orderlyMsgs);
				reflectionTuple.handleAsync(invokeList);
			}
			
		});
	}

	/**
	 * 按给定的参数表获取到全部可用的handler(以及对应的位图)
	 * 
	 * @param fullPS
	 * @param messages
	 * @return
	 */
	private final Map<Long, List<MessageHandlerReflectionTuple>> getProxyAsyncMultiHandlers(String fullPS, IMessage... messages) {

		return MapUtil.getOrAdd(handlerMoreThanOneLocater, fullPS, () -> {
			
			Map<Long, List<MessageHandlerReflectionTuple>> bitLocator = new HashMap<>();

			Set<String> pSet = getStringDescOrderly(m -> m.getClass().getName(), messages);
			
			int pAmount = messages.length;
			Class<?>[] typeList = new Class<?>[pAmount];
			int i = 0;
			for(String typeName : pSet) {
				try {
					typeList[i] = Class.forName(typeName);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				i ++ ;
			}
			
			Map<Long, List<Class<?>>> bitGraph = new HashMap<>(); 
			int bitAll = (1 << pAmount) - 1;
			bitGraph.put(new Long(bitAll), Arrays.asList(typeList));
			
			// eg. pAmount = 3 那么 x -> { 1 ,2 ,3 ,4, 5, 6 }
			//     x 对应的二进制取值为 { 001, 010, 011, 100, 101, 110 }
			//     集合有序且元素固定的情况下 1 即为选中， 0则未选中， 这样正好取出除空集(000)和全集(111)外的所有子集
			// 这段循环里采用倒叙 即 { 6, 5, 4, 3, 2, 1 } ，这样比正序节省很多中间变量和控制代码
			// 如果初始条件中把-1去掉，即 111 也考虑进来，那么全集也会被计算到子集中
			// 如果循环判断中x>0改为x>=0，即 000 也考虑进来，那么空集也会被计算到子集中
			for(int x=bitAll-1; x>0; x--) {
				List<Class<?>> subSet = new ArrayList<>(); // @important 必须使用有序表!
				int index = 1;
				int px = 0;
				do {
					if((x & index) > 0)
							subSet.add(typeList[px]);
					px++;
				} while((index <<= 1) <= x);
				bitGraph.put(new Long(x), subSet);
			}
			
			EachUtilx.forEach(bitGraph, (b, l) -> {
				StringBuilder pSignature = new StringBuilder();
				EachUtilx.forEach(l, t -> pSignature.append(t.getName()));
				String pS = pSignature.toString();
				List<MessageHandlerReflectionTuple> invokerList = handlerFullContraintMapper.get(pS);
				
				if(null != invokerList)
					bitLocator.put(b, invokerList);
			});
			
			return bitLocator;
		});
	}
	

	/**
	 * 把有续集里的所有字符串一次连接起来。
	 * @param pSet
	 * @return
	 */
	private static String getFullPs(Set<String> pSet) {
		StringBuilder fullPSignature = new StringBuilder();
		EachUtilx.forEach(pSet, fullPSignature::append);
		return fullPSignature.toString();
	}

	/**
	 * 使用stringer绑定的获取对象的字符串方法获得字符串并使用ascii序排成一个集合(TreeSet)
	 * 
	 * @param stringer
	 * @param targets
	 * @return
	 */
	@SafeVarargs
	private static <O> Set<String> getStringDescOrderly(IFunction1<String, O> stringer, O... targets) {
		Set<String> pSet = new TreeSet<>((s1, s2) -> s1.compareTo(s2));
		for(O t : targets)
			pSet.add(stringer.trigger(t)); // 把所有参数的全限定名称放到有序集合中
		return pSet;
	}
	
	/**
	 * 按照位图从参数表中拼出调用序列表
	 * 
	 * @param bitGraph 位图
	 * @param asciiOrder 调用序和ascii序的映射关系
	 * @param messages 参数表 (调用此方法钱，必须按照类型的全限定名称的ascii序排好)
	 * @return
	 */
	private static IMessage[] contructPTable(long bitGraph, List<Integer> asciiOrder, IMessage... messages) {
		List<IMessage> tmpPList = new ArrayList<>();
		int index = 1;
		int px = 0;
		do {
			if((bitGraph & index) > 0)
				tmpPList.add(messages[px]);
			px++;
		} while((index <<= 1) <= bitGraph) ;
		IMessage[] orderlyPList = tmpPList.toArray(tRef);
		if(null == asciiOrder)
			return orderlyPList;
		IMessage[] invokePList = new IMessage[orderlyPList.length];
		for(int x=0; x<asciiOrder.size(); x++) {
			invokePList[asciiOrder.get(x)] = orderlyPList[x];
		}
		return invokePList;
	}
	
	private final static IMessage[] tRef = new IMessage[0];
	
	public static class MessageHandlerReflectionTuple implements IMessageHandlerProxy {
		
		public final Class<? extends IMessageHandler> handlerClass;
		
		public final Method handleReflectionMethod;
		
		public final String identification;
		
		public final IEjokerContextDev2 ejokerContext;
		
		public final List<Integer> asciiOrder;
		
		private IMessageHandler realHandler = null;
		
		public MessageHandlerReflectionTuple(Method handleReflectionMethod, String pList, IFunction<IEjokerContextDev2> ejokerProvider) {
			this(handleReflectionMethod, pList, null, ejokerProvider);
		}

		public MessageHandlerReflectionTuple(Method handleReflectionMethod, String pList, List<Integer> asciiOrder, IFunction<IEjokerContextDev2> ejokerProvider) {
			this.handleReflectionMethod = handleReflectionMethod;
			this.handlerClass = (Class<? extends IMessageHandler> )handleReflectionMethod.getDeclaringClass();
			identification = StringUtilx.fill("Proxy::{}#{}({})",
					handlerClass.getSimpleName(),
					MessageHandlerPool.HANDLER_METHOD_NAME,
					pList);
			this.ejokerContext = ejokerProvider.trigger();
			
			this.asciiOrder = asciiOrder;
		}

		@Override
		public IMessageHandler getInnerObject() {
			if(null == realHandler)
				return realHandler = ejokerContext.get(handlerClass);
			return realHandler;
		}

		@Override
		public Future<AsyncTaskResult<Void>> handleAsync(IMessage... messages) {
			try {
				return (Future<AsyncTaskResult<Void>> )handleReflectionMethod.invoke(getInnerObject(), messages);
			} catch (IllegalAccessException|IllegalArgumentException e) {
				String fullPs = MessageHandlerPool.getFullPs(MessageHandlerPool.getStringDescOrderly(m -> m.getClass().getSimpleName(), messages));
				throw new RuntimeException("Message handle failed!!! " + fullPs, e);
			} catch (InvocationTargetException e) {
				throw new AsyncWrapperException(e.getCause());
			}
		}

		@Override
		public String toString() {
			return identification;
		}
	}

}
