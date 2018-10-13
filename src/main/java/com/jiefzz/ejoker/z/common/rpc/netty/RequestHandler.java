package com.jiefzz.ejoker.z.common.rpc.netty;

import com.jiefzz.ejoker.z.common.system.functional.IVoidFunction1;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public class RequestHandler extends SimpleChannelInboundHandler<String> {
	
	public IVoidFunction1<String> action;
	
	public RequestHandler(IVoidFunction1<String> action) {
		this.action = action;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        
        action.trigger(msg);
        
        ctx.writeAndFlush('\n');
	}
}
