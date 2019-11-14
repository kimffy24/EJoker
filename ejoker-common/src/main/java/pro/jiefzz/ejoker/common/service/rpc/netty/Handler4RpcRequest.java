package pro.jiefzz.ejoker.common.service.rpc.netty;

import io.netty.channel.ChannelHandler.Sharable;
import pro.jiefzz.ejoker.common.system.functional.IVoidFunction1;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public class Handler4RpcRequest extends SimpleChannelInboundHandler<String> {
	
	public IVoidFunction1<String> action;
	
	public Handler4RpcRequest(IVoidFunction1<String> action) {
		this.action = action;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        
        action.trigger(msg);
        ctx.writeAndFlush('\n');
        
	}
}
