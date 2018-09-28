package com.jiefzz.ejoker.z.common.rpc.netty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jiefzz.ejoker.z.common.action.Action;
import com.jiefzz.ejoker.z.common.context.annotation.context.Dependence;
import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.io.AsyncTaskResultBase;
import com.jiefzz.ejoker.z.common.io.AsyncTaskStatus;
import com.jiefzz.ejoker.z.common.io.IOExceptionOnRuntime;
import com.jiefzz.ejoker.z.common.io.IOHelper;
import com.jiefzz.ejoker.z.common.io.IOHelper.IOActionExecutionContext;
import com.jiefzz.ejoker.z.common.rpc.IRPCService;
import com.jiefzz.ejoker.z.common.system.extension.acrossSupport.RipenFuture;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

@EService
public class NettyRPCServiceImpl implements IRPCService {

	private final static Logger logger = LoggerFactory.getLogger(NettyRPCServiceImpl.class);

	@Dependence
	IOHelper ioHelper;
	
	@Override
	public void export(final Action<String> action, final int port) {
		if (portMap.containsKey(port)) {
			throw new RuntimeException(String.format("Another action has registed on port %d!!!", port));
		}

		rpcRegistLock.lock();
		try {
			Thread ioThread = new Thread(() -> {
					EventLoopGroup bossGroup = new NioEventLoopGroup();
					EventLoopGroup workerGroup = new NioEventLoopGroup();
					try {
						ServerBootstrap b = new ServerBootstrap();
						b.group(bossGroup, workerGroup);
						b.channel(NioServerSocketChannel.class);
						b.childHandler(new EJokerServerInitializer(new RequestHandler(action)));

						// 服务器绑定端口监听
						ChannelFuture f = b.bind(port).sync();
						// 监听服务器关闭监听
						f.channel().closeFuture().sync();

					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						bossGroup.shutdownGracefully();
						workerGroup.shutdownGracefully();
					}
				}
			);
			ioThread.start();
			portMap.put(port, new RPCTuple(action, ioThread));
		} finally {
			rpcRegistLock.unlock();
		}
	}

	@Override
	public void removeExport(int port) {
		RPCTuple rpcTuple;
		if (null != (rpcTuple = portMap.remove(port))) {
			rpcTuple.ioThread.interrupt();
		}
	}

	@Override
	public void remoteInvoke(final String data, final String host, final int port) {
		
		ioHelper.tryAsyncAction(new IOActionExecutionContext<AsyncTaskResultBase>() {

			private String actName = String.format("remoteInvoke[target: %s:%d]", host, port);
			
			@Override
			public String getAsyncActionName() {
				return actName;
			}

			/**
			 * 
			 * TODO 1. 此处目前还是java原生实现，有空记得改为netty
			 * TODO 2. 此处本质上是同步执行然后返回一个异步任务读取资源的结构对象给调用者，并非真正的异步
			 */
			@Override
			public Future<AsyncTaskResultBase> asyncAction() throws IOException {
				Socket socket = null;
				try {
					socket = new Socket(host, port);// 创建一个客户端连接
					OutputStream out = socket.getOutputStream();// 获取服务端的输出流，为了向服务端输出数据
					// InputStream in = socket.getInputStream();//
					// 获取服务端的输入流，为了获取服务端输入的数据

					PrintWriter bufw = new PrintWriter(out, true);
					bufw.println(data);// 发送数据给服务端
					bufw.flush();

					bufw.close();
					out.close();
					bufw = null;
					out = null;
				} catch (Exception e) {
					e.printStackTrace();
					throw IOExceptionOnRuntime.encapsulation(e);
				} finally {
					if(null!=socket) {
						socket.close();
						socket = null;
					}
				}

				RipenFuture<AsyncTaskResultBase> ripenFuture = new RipenFuture<AsyncTaskResultBase>();
				ripenFuture.trySetResult(new AsyncTaskResultBase(AsyncTaskStatus.Success));
				return ripenFuture;
			}

			@Override
			public void faildLoopAction() {
				ioHelper.tryAsyncAction(this);
			}

			@Override
			public void finishAction(AsyncTaskResultBase result) {
				
			}

			@Override
			public void faildAction(Exception ex) {
				logger.error("failedAction invoke! parameter: {}", ex.getMessage());
			}});
	}
}
