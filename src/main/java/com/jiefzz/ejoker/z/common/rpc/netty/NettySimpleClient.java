package com.jiefzz.ejoker.z.common.rpc.netty;

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

public class NettySimpleClient {

	private final static Logger logger = LoggerFactory.getLogger(NettySimpleClient.class);
	
	private final String clientDesc;
	
	private long lostInvokeTime;

	private EventLoopGroup eventLoopGroup;

	private SocketChannel socketChannel;

	public NettySimpleClient(String host, int port) {
		
		eventLoopGroup = new NioEventLoopGroup();
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

						// 以("\n")为结尾分割的 解码器
						pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));

						// 字符串解码 和 编码
						pipeline.addLast("decoder", new StringDecoder());
						pipeline.addLast("encoder", new StringEncoder());

						pipeline.addLast("handler", new Handler4RpcClient());
					}
				});
		// 进行连接
		ChannelFuture future = bootstrap.connect(host, port).awaitUninterruptibly();
		// 判断是否连接成功
		if (future.isSuccess()) {
			logger.debug("Client[to: {}:{} ] create success ...", host, port);
			// 得到管道，便于通信
			socketChannel = (SocketChannel) future.channel();
		} else {
			logger.debug("Client[to: {}:{} ] create faild ...", host, port);
		}

		clientDesc = host + ":" + port;
		lostInvokeTime = System.currentTimeMillis();
	}

	public void sendMessage(Object msg) {
		if (socketChannel == null) {
			throw new RuntimeException("Not avaliable!!!");
		}
		logger.debug("send: {}", msg);
		socketChannel.writeAndFlush(msg);
		lostInvokeTime = System.currentTimeMillis();
	}

	public void close() {
		if (socketChannel == null) {
			throw new RuntimeException("Not avaliable!!!");
		}
		socketChannel.closeFuture().awaitUninterruptibly();
		// 优雅地退出，释放相关资源
		eventLoopGroup.shutdownGracefully();
	}
	
	@Override
	public String toString() {
		return clientDesc;
	}
	
	public boolean isInactive(long atLast) {
		return System.currentTimeMillis() - lostInvokeTime - atLast > 0l;
	}
}
