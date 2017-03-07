package com.jiefzz.ejoker.z.common.rpc.netty;

import com.jiefzz.ejoker.z.common.action.Action;
import com.jiefzz.ejoker.z.common.rpc.IRPCService;

public class AbstractNettyRPCServiceImpl<TType> implements IRPCService<TType> {

	@Override
	public void export(Action<TType> action, int port) {
		if(null != portMap.putIfAbsent(port, action)) {
			throw new RuntimeException(String.format("Another action has registed on port %d!!!", port));
		}
		
		
		
	}

	@Override
	public void remoteInvoke(TType data, String host, int port) {
	}

}
