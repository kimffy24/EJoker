package pro.jiefzz.ejoker.common.service.rpc.netty;

import static pro.jiefzz.ejoker.common.system.extension.LangUtil.await;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import pro.jiefzz.ejoker.common.system.helper.StringHelper;
import pro.jiefzz.ejoker.common.system.task.io.IOExceptionOnRuntime;
import pro.jiefzz.ejoker.common.system.wrapper.CountDownLatchWrapper;

public class NettySimpleClient {

	private final static Logger logger = LoggerFactory.getLogger(NettySimpleClient.class);
	
	private final String clientDesc;
	
	private long lastInvokeTime = System.currentTimeMillis();
	
	private final AtomicBoolean ready = new AtomicBoolean(false);
	
	private final Object connectBlocker;

	private EventLoopGroup eventLoopGroup;

	private SocketChannel socketChannel;

	public NettySimpleClient(String host, int port) {

		clientDesc = host + ":" + port;
		connectBlocker = CountDownLatchWrapper.newCountDownLatch();
		eventLoopGroup = new NioEventLoopGroup();
		
		
		Thread clientHolder = new Thread(() -> {
			try {
				Bootstrap bootstrap = new Bootstrap();
				bootstrap.channel(NioSocketChannel.class)
						// 保持连接
						.option(ChannelOption.SO_KEEPALIVE, true)
						// 有数据立即发送
						.option(ChannelOption.TCP_NODELAY, true)
						// 绑定处理group
						.group(eventLoopGroup).remoteAddress(host, port).handler(new ChannelInitializer<SocketChannel>() {
							@Override
							protected void initChannel(SocketChannel socketChannel) throws Exception {
	
								ChannelPipeline pipeline = socketChannel.pipeline();
	
						        // 以("\n" or "\r\n")为结尾分割的 解码器
								pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
	
								// 字符串解码 和 编码
								pipeline.addLast("decoder", new StringDecoder());
								pipeline.addLast("encoder", new StringEncoder());
	
								pipeline.addLast("handler", new Handler4RpcClient());
							}
						});
				// 进行连接
				ChannelFuture future = bootstrap.connect(host, port).awaitUninterruptibly();
				lastInvokeTime = System.currentTimeMillis();
				await(future);
				// 判断是否连接成功
				if (future.isSuccess()) {
					logger.debug("Client[to: {}:{} ] create success ...", host, port);
					// 得到管道，便于通信
					socketChannel = (SocketChannel )future.channel();
					ready.set(true);
					CountDownLatchWrapper.countDown(connectBlocker);
					socketChannel.closeFuture().awaitUninterruptibly();
				} else {
					logger.debug("Client[to: {}:{} ] create faild ...", host, port);
					CountDownLatchWrapper.countDown(connectBlocker);
				}
			} finally {
				eventLoopGroup.shutdownGracefully();
			}
		}, "rpc:client:" + clientDesc);
		clientHolder.setDaemon(true);
		clientHolder.start();

		lastInvokeTime = System.currentTimeMillis();
	}
	
	public void awaitReady() {
		lastInvokeTime = System.currentTimeMillis();
		
		CountDownLatchWrapper.awaitInterruptable(connectBlocker);
		if(!ready.get()) {
			throw new IOExceptionOnRuntime(new IOException(StringHelper.fill("Clien create faild!!! [connectTo: {}] ", clientDesc)));
		}
	}

	public void sendMessage(Object msg) {
		if (socketChannel == null) {
			throw new RuntimeException("Not avaliable!!!");
		}
		socketChannel.writeAndFlush(msg);
		lastInvokeTime = System.currentTimeMillis();
	}

	public void close() {
		if (socketChannel != null) {
			socketChannel.close();
		}
	}
	
	@Override
	public String toString() {
		return clientDesc;
	}
	
	public boolean isInactive(long atLast) {
		return System.currentTimeMillis() - lastInvokeTime - atLast > 0l;
	}
}
