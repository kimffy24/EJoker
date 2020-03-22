package pro.jiefzz.ejoker.common.service.rpc.netty;

import static pro.jiefzz.ejoker.common.system.extension.LangUtil.await;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import pro.jiefzz.ejoker.common.context.annotation.context.Dependence;
import pro.jiefzz.ejoker.common.context.annotation.context.EInitialize;
import pro.jiefzz.ejoker.common.context.annotation.context.EService;
import pro.jiefzz.ejoker.common.service.IScheduleService;
import pro.jiefzz.ejoker.common.service.Scavenger;
import pro.jiefzz.ejoker.common.service.rpc.IRPCService;
import pro.jiefzz.ejoker.common.system.enhance.EachUtilx;
import pro.jiefzz.ejoker.common.system.enhance.MapUtilx;
import pro.jiefzz.ejoker.common.system.enhance.StringUtilx;
import pro.jiefzz.ejoker.common.system.extension.acrossSupport.EJokerFutureTaskUtil;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import pro.jiefzz.ejoker.common.system.task.io.IOHelper;
import pro.jiefzz.ejoker.common.system.wrapper.DiscardWrapper;

@EService
public class NettyRPCServiceImpl implements IRPCService {

	private final static Logger logger = LoggerFactory.getLogger(NettyRPCServiceImpl.class);

	private final static long clientInactiveMilliseconds = 15000l;
//	private final static long clientInactiveMilliseconds = Long.MAX_VALUE;
	
	@Dependence
	private Scavenger scavenger;
	
	@Dependence
	private IOHelper ioHelper;

	@Dependence
	private IScheduleService scheduleService;
	
	private final Map<String, AtomicBoolean> clientConnectionOccupation = new HashMap<>();
	
	private Map<Integer, IVoidFunction> closeHookTrigger = new HashMap<>();
	
	private Map<String, NettySimpleClient> clientStore = new HashMap<>();
	
	@EInitialize
	private void init() {
		scavenger.addFianllyJob(this::exitHook);
		scheduleService.startTask(StringUtilx.fmt("{}@{}#{}", this.getClass().getName(), this.hashCode(), "cleanInactiveClient()"),
				this::cleanInactiveClient,
				2000l,
				2000l);
	}
	
	// @unsafe on multiple thread process
	@Override
	public void export(IVoidFunction1<String> action, int port, boolean waitFinished) {
		if (portMap.containsKey(port)) {
			throw new RuntimeException(StringUtilx.fmt("Port conflict!!! [port: {}]!!!", port));
		}

		RPCTuple currentTuple = null;
		AtomicBoolean ab = MapUtilx.getOrAdd(serverPortOccupation, port, () -> new AtomicBoolean());
		if(ab.compareAndSet(false, true)) {
			Thread ioThread = new Thread(
					() -> {
						EventLoopGroup bossGroup = new NioEventLoopGroup();
						EventLoopGroup workerGroup = new NioEventLoopGroup();
						// 期间，此线程应该会一直等待。
						try {
							ServerBootstrap b = new ServerBootstrap();
							b.group(bossGroup, workerGroup);
							b.channel(NioServerSocketChannel.class);
							b.childHandler(new EJokerServerInitializer(new Handler4RpcRequest(action)));
			
							// 服务器绑定端口监听
							ChannelFuture f = b.bind(port).awaitUninterruptibly();
							
							{
								// sync 协调逻辑
								closeHookTrigger.put(port, f.channel()::close);
								RPCTuple currentRPCTuple;
								while(null == (currentRPCTuple = portMap.get(port)))
									DiscardWrapper.sleepInterruptable(TimeUnit.MILLISECONDS, 5l);
								currentRPCTuple.initialFuture.trySetResult(null);
							}					
							// 监听服务器关闭监听
							f.channel().closeFuture().awaitUninterruptibly();
						} catch (RuntimeException e) {
							{
								// sync 协调逻辑
								RPCTuple currentRPCTuple;
								while(null == (currentRPCTuple = portMap.get(port)))
									;
								currentRPCTuple.initialFuture.trySetException(e);
							}
						} finally {
							bossGroup.shutdownGracefully();
							workerGroup.shutdownGracefully();
						}
					},
				"rcp:listener:" + port);
			portMap.put(port, currentTuple = new RPCTuple(action, ioThread));
			// sync: start一定要放在注册portMap之后进行。
			ioThread.start();
			if(waitFinished)
				await(currentTuple.initialFuture);
		} else {
			if(waitFinished) {
				while(null == (currentTuple = portMap.get(port)))
					DiscardWrapper.sleepInterruptable(TimeUnit.MILLISECONDS, 10l);
			}
		}
			
	}

	// @unsafe on multiple thread process
	@Override
	public void removeExport(int port) {
		AtomicBoolean atomicBoolean = MapUtilx.getOrAdd(serverPortOccupation, port, () -> new AtomicBoolean());
		if(!atomicBoolean.compareAndSet(true, false))
			return;
		IVoidFunction closeAction = closeHookTrigger.remove(port);
		if(null == closeAction)
			return;
		closeAction.trigger();
		portMap.remove(port);
	}

	@Override
	public void remoteInvoke(String data, String host, int port) {
		fetchNettySimpleClient(host, port);
		remoteInvokeInternal(host, port, data);
	}
	
	private NettySimpleClient fetchNettySimpleClient(String host, int port) {
		String uniqueKey = host+":"+port;
		NettySimpleClient nettySimpleClient = clientStore.get(uniqueKey);
		while(null == nettySimpleClient) {
			AtomicBoolean acquired = MapUtilx.getOrAdd(clientConnectionOccupation, uniqueKey, () -> new AtomicBoolean());
			if(acquired.compareAndSet(false, true)) {
				nettySimpleClient = new NettySimpleClient(host, port);
				nettySimpleClient.awaitReady();
				clientStore.put(uniqueKey, nettySimpleClient);
			} else {
				int loop = 0;
				while (loop++<5 && null == (nettySimpleClient = clientStore.get(uniqueKey)))
					DiscardWrapper.sleepInterruptable(TimeUnit.MILLISECONDS, 20l);
				if(null == nettySimpleClient) {
					continue;
				}
			}
			break;
		}
		return nettySimpleClient;
	}
	
	private void remoteInvokeInternal(String host, int port, String data) {
		final String s;
		int dIndexOf = data.lastIndexOf('\n');
		if(data.length() - dIndexOf -1 != 0)
			s = data + "\n";
		else
			s = data;
		
		ioHelper.tryAsyncAction2(
				"RemoteInvoke",
				() -> {
					fetchNettySimpleClient(host, port).sendMessage(s);
					return EJokerFutureTaskUtil.completeTask();
				},
				() -> {},
				() -> StringUtilx.fmt("RemoteInvoke::{}:{}", host, port),
				e -> logger.error("Send data to remote host faild!!! [remoteAddress: {}, port: {}, data: {}]", host, port, data, e),
				true);
	}
	
	private void cleanInactiveClient() {
		Iterator<Entry<String, NettySimpleClient>> iterator = clientStore.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, NettySimpleClient> current = iterator.next();
			String clientIdentify = current.getKey();
			NettySimpleClient client = current.getValue();
			if(!client.isInactive(clientInactiveMilliseconds))
				continue;
			iterator.remove();
			AtomicBoolean ab = clientConnectionOccupation.get(clientIdentify);
			if(null != ab)
				ab.set(false);
			client.close();
			logger.debug("Close rpc client: {}", clientIdentify);
		}
	}
	
	// @unsafe
	private void exitHook() {
		
		// TODO 没关注clientConnectionOccupation和serverPortOccupation
		clientConnectionOccupation.clear();
		serverPortOccupation.clear();
		
		EachUtilx.forEach(clientStore, (k, c) -> {
			logger.debug("Close netty rpc client {}", k);
			c.close();
		});
		clientStore.clear();
		
		EachUtilx.forEach(closeHookTrigger, (p, a) -> {
			a.trigger();
			portMap.remove(p);
		});
		closeHookTrigger.clear();
	}
}
