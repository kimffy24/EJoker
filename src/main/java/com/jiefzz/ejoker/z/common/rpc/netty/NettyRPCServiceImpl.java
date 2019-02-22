package com.jiefzz.ejoker.z.common.rpc.netty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.rpc.IRPCService;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.helper.ForEachHelper;
import com.jiefzz.ejoker.z.common.system.helper.MapHelper;
import com.jiefzz.ejoker.z.common.system.wrapper.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerTaskAsyncHelper;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

@EService
public class NettyRPCServiceImpl implements IRPCService {

	private final static Logger logger = LoggerFactory.getLogger(NettyRPCServiceImpl.class);

	private final static long clientInactiveMilliseconds = 15000l;
	
	@Dependence
	private Scavenger scavenger;
	
	@Dependence
	private IOHelper ioHelper;

	@Dependence
	private IScheduleService scheduleService;
	
	@Dependence
	private EJokerTaskAsyncHelper eJokerAsyncHelper;
	
	private final Map<String, AtomicBoolean> clientConnectionOccupation = new HashMap<>();
	
	private Map<Integer, IVoidFunction> closeHookTrigger = new HashMap<>();
	
	private Map<String, NettySimpleClient> clientStore = new HashMap<>();
	
	@EInitialize
	private void init() {
		scavenger.addFianllyJob(this::exitHook);
		scheduleService.startTask(String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveClient()"),
				this::cleanInactiveClient,
				1000l,
				1000l);
	}
	
	// @unsafe
	@Override
	public void export(IVoidFunction1<String> action, int port, boolean waitFinished) {
		if (portMap.containsKey(port)) {
			throw new RuntimeException(String.format("Another action has registed on port %d!!!", port));
		}

		RPCTuple currentTuple = null;
		AtomicBoolean ab = MapHelper.getOrAdd(serverPortOccupation, port, AtomicBoolean::new);
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
									SleepWrapper.sleep(TimeUnit.MILLISECONDS, 5l);
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
		} else {
			if(waitFinished) {
				while(null == (currentTuple = portMap.get(port)))
					SleepWrapper.sleep(TimeUnit.MILLISECONDS, 10l);
			}
		}
		
		if(waitFinished)
			currentTuple.initialFuture.get();
			
	}

	// @unsafe
	@Override
	public void removeExport(int port) {
		AtomicBoolean atomicBoolean = serverPortOccupation.get(port);
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
		if(null == nettySimpleClient) {
			AtomicBoolean acquired = MapHelper.getOrAdd(clientConnectionOccupation, uniqueKey, AtomicBoolean::new);
			if(acquired.compareAndSet(false, true)) {
				nettySimpleClient = new NettySimpleClient(host, port);
				clientStore.put(host+":"+port, nettySimpleClient);
			} else {
				int loop = 0;
				while (loop++<3 && null == (nettySimpleClient = clientStore.get(uniqueKey)))
					SleepWrapper.sleep(TimeUnit.MILLISECONDS, 50l);
				if(null == nettySimpleClient) {
					return fetchNettySimpleClient(host, port);
				}
			}
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
				() -> eJokerAsyncHelper.submit(() -> fetchNettySimpleClient(host, port).sendMessage(s)),
				r -> {},
				() -> String.format("remoteInvoke[target: %s:%d]", host, port),
				e -> logger.error(String.format("Send data to remote host faild!!! remoteAddress: %s:%d, data: %s", host, port, data)),
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
		
		ForEachHelper.processForEach(clientStore, (k, c) -> {
			logger.debug("Close netty rpc client {}", k);
			c.close();
		});
		clientStore.clear();
		
		ForEachHelper.processForEach(closeHookTrigger, (p, a) -> {
			a.trigger();
			portMap.remove(p);
		});
		closeHookTrigger.clear();
	}
}
