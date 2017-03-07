package com.jiefzz.ejoker.z.common.rpc;

import java.util.HashMap;
import java.util.Map;

import com.jiefzz.ejoker.z.common.action.Action;

/**
 * 不使用Java对象动态代理技术。
 * <br> 框架上下文内，同意端口，就约定同一类对象调用，方式、参数类型都相同。
 */
public interface IRPCService<TType> {
	
	final static Map<Integer, Action<?>> portMap = new HashMap<Integer, Action<?>>();

	public void export(Action<TType> action, final int port);
	
	public void remoteInvoke(TType data, final String host, final int port);
}
