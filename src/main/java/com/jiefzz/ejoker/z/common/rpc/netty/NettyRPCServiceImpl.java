package com.jiefzz.ejoker.z.common.rpc.netty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EInitialize;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResult;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.io.IOHelper.IOActionExecutionContext;
import com.jiefzz.ejoker.z.common.rpc.IRPCService;
import com.jiefzz.ejoker.z.common.scavenger.Scavenger;
import com.jiefzz.ejoker.z.common.schedule.IScheduleService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.SystemFutureWrapper;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction;
import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;
import com.jiefzz.ejoker.z.common.system.wrapper.threadSleep.SleepWrapper;
import com.jiefzz.ejoker.z.common.task.context.EJokerAsyncHelper;
import com.jiefzz.ejoker.z.common.utils.ForEachUtil;

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
	private EJokerAsyncHelper eJokerAsyncHelper;
	
	private Lock lock4CreateClient = new ReentrantLock();
	
	private Map<Integer, IVoidFunction> closeHookTrigger = new HashMap<>();
	
	private Map<String, NettySimpleClient> clientStore = new HashMap<>();
	
	@EInitialize
	private void init() {
		scavenger.addFianllyJob(this::exitHook);
		scheduleService.startTask(String.format("%s@%d#%s", this.getClass().getName(), this.hashCode(), "cleanInactiveClient()"),
				this::cleanInactiveClient,
				800l,
				800l);
	}
	
	@Override
	public void export(final IVoidFunction1<String> action, final int port, boolean waitFinished) {
		if (portMap.containsKey(port)) {
			throw new RuntimeException(String.format("Another action has registed on port %d!!!", port));
		}
		RPCTuple currentTuple = null;
		rpcRegistLock.lock();
		try {
			Thread ioThread = new Thread(() -> {
					EventLoopGroup bossGroup = new NioEventLoopGroup();
					EventLoopGroup workerGroup = new NioEventLoopGroup();
					try {
						ServerBootstrap b = new ServerBootstrap();
						b.group(bossGroup, workerGroup);
						b.channel(NioServerSocketChannel.class);
						b.childHandler(new EJokerServerInitializer(new Handler4RpcRequest(action)));

						// 服务器绑定端口监听
						ChannelFuture f = b.bind(port).sync();
						
						{
							// sync 协调逻辑
							closeHookTrigger.put(port, f.channel()::close);
							RPCTuple currentRPCTuple;
							while(null == (currentRPCTuple = portMap.get(port)))
								SleepWrapper.sleep(TimeUnit.MILLISECONDS, 50l);
							currentRPCTuple.initialFuture.trySetResult(null);
						}
						
						// 监听服务器关闭监听
						f.channel().closeFuture().sync();
						// 期间，此线程应该会一直等待。

					} catch (Exception e) {
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
				}
			, "rcp:listener:" + port);
			portMap.put(port, currentTuple = new RPCTuple(action, ioThread));
			// sync: start一定要放在注册portMap之后进行。
			ioThread.start();
		} finally {
			rpcRegistLock.unlock();
		}
		
		if(waitFinished)
			currentTuple.initialFuture.get();
			
	}

	// @unsafe
	@Override
	public void removeExport(int port) {
		rpcRegistLock.lock();
		try {
			IVoidFunction closeAction = closeHookTrigger.remove(port);
			if(null == closeAction)
				return;
			closeAction.trigger();
			portMap.remove(port);
		} finally {
			rpcRegistLock.unlock();
		}
	}

	@Override
	public void remoteInvoke(final String data, final String host, final int port) {
		
		NettySimpleClient nettySimpleClient = clientStore.get(host+":"+port);
		if(null == nettySimpleClient) {
			lock4CreateClient.lock();
			try {
				if(null == (nettySimpleClient = clientStore.get(host+":"+port))) {
					nettySimpleClient = new NettySimpleClient(host, port);
					clientStore.put(host+":"+port, nettySimpleClient);
				}
			} finally {
				lock4CreateClient.unlock();
			}
		}
		remoteInvokeInternal(nettySimpleClient, data);
	}
	
	private void remoteInvokeInternal(NettySimpleClient client, String data) {
		ioHelper.tryAsyncAction(new IOActionExecutionContext<Void>(true) {

			@Override
			public String getAsyncActionName() {
				return "RemoteInvoke";
			}

			@Override
			public SystemFutureWrapper<AsyncTaskResult<Void>> asyncAction() throws Exception {
				
				String s;
				int dIndexOf = data.lastIndexOf('\n');
				if(data.length() - dIndexOf -1 != 0)
					s = data + "\n";
				else
					s = data;
				
				return eJokerAsyncHelper.submit(() -> client.sendMessage(s));
			}

			@Override
			public void finishAction(Void result) {
				// do nothing.
			}

			@Override
			public void faildAction(Exception ex) {
				logger.error(String.format("Send data to remote host faild!!! remoteAddress: %s, data: %s", client.toString(), data));
			}

			@Override
			public String getContextInfo() {
				return String.format("remoteInvoke[target: %s]", client.toString());
			}
			
		});
	}
	
	private void cleanInactiveClient() {
		Iterator<Entry<String, NettySimpleClient>> iterator = clientStore.entrySet().iterator();
		while(iterator.hasNext()) {
			Entry<String, NettySimpleClient> current = iterator.next();
			if(!current.getValue().isInactive(clientInactiveMilliseconds))
				continue;
			iterator.remove();
			logger.debug("Close rpc client: {}", current.getKey());
			try {
				current.getValue().close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void exitHook() {
		
		ForEachUtil.processForEach(clientStore, (k, c) -> {
			logger.debug("Close netty rpc client {}", k);
			try {
				c.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		clientStore.clear();
		
		ForEachUtil.processForEach(closeHookTrigger, (p, a) -> {
			a.trigger();
			portMap.remove(p);
		});
		closeHookTrigger.clear();
	}
}
