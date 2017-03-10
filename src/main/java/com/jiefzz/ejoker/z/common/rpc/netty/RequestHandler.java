package com.jiefzz.ejoker.z.common.rpc.netty;

import com.jiefzz.ejoker.z.common.action.Action;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public class RequestHandler extends SimpleChannelInboundHandler<String> {
	
	public Action<String> action;
	
	public RequestHandler(Action<String> action) {
		this.action = action;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        
        action.execute(msg);
        
        ctx.writeAndFlush('\n');
	}
}
