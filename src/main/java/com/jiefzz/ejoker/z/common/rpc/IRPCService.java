package com.jiefzz.ejoker.z.common.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.z.common.action.Action;

/**
 * 不使用Java对象动态代理技术。
 * <br> 框架上下文内，同意端口，就约定同一类对象调用，方式、参数类型都相同。
 */
public interface IRPCService {
	
	final static Map<Integer, RPCTuple> portMap = new HashMap<>();
	
	final static Lock rpcRegistLock = new ReentrantLock();

	public void export(Action<String> action, int port);
	
	public void remoteInvoke(String data, String host, int port);
	
	public void removeExport(int port);

	public static class RPCTuple {
		
		public final Thread ioThread;
		
		public final Action<?> action;
		
		public RPCTuple(Action<?> action, Thread ioThread) {
			this.ioThread = ioThread;
			this.action = action;
		}
		
	}
}
