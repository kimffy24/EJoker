package com.jiefzz.ejoker.z.common.rpc.simpleRPC;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Remote Procedure Call <br >
 * @author kimffy
 *
 */
public class RPCFramework {  
	// 复制自http://javatar.iteye.com/blog/1123915
	
    /** 
     * regist Service Object
     * @param service 服务实现 
     * @param port 服务端口 
     * @throws Exception 
     */  
    public static void export(final Object service, final int port) throws Exception {  
        if (service == null)  
            throw new IllegalArgumentException("service instance == null");  
        if (port < 1024 || port > 65535)  
            throw new IllegalArgumentException("Invalid port " + port);
        reflectAnalyzeAndCacheInfo(service.getClass(), port);
        ServerSocket server = new ServerSocket(port);  
        for(;;) {
            try {  
                final Socket socket = server.accept();  
                new Thread(new Runnable() {  
                    @Override  
                    public void run() {  
                        try {  
                            try {  
                                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());  
                                try {  
                                    String methodName = input.readUTF();  
                                    Class<?>[] parameterTypes = (Class<?>[] )input.readObject();
                                    String methodSignature = getMethodSignature(parameterTypes);
                                    Object[] arguments = (Object[] )input.readObject();  
                                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());  
                                    try {  
                                        //Method method = service.getClass().getMethod(methodName, parameterTypes); 
                                    	Method method = ExportEPCServiceClassInfo.get(port).get(methodName +parameterTypes);
                                        Object result = method.invoke(service, arguments);  
                                        output.writeObject(result);  
                                    } catch (Throwable t) {  
                                        output.writeObject(t);  
                                    } finally {  
                                        output.close();  
                                    }  
                                } finally {  
                                    input.close();  
                                }  
                            } finally {  
                                socket.close();  
                            }  
                        } catch (Exception e) {  
                            e.printStackTrace();  
                        }  
                    }  
                }).start();  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
    }  
  
    /** 
     * remote invoke
     * @param <T> 接口泛型 
     * @param interfaceClass 接口类型 
     * @param host 服务器主机名 
     * @param port 服务器端口 
     * @return 远程服务 
     * @throws Exception 
     */  
    @SuppressWarnings("unchecked")  
    public static <T> T refer(final Class<T> interfaceClass, final String host, final int port) throws Exception {  
        if (interfaceClass == null)  
            throw new IllegalArgumentException("Interface class == null");  
        if (! interfaceClass.isInterface())  
            throw new IllegalArgumentException("The " + interfaceClass.getName() + " must be interface class!");  
        if (host == null || host.length() == 0)  
            throw new IllegalArgumentException("Host == null!");
        if (port < 1024 || port > 65535)  
            throw new IllegalArgumentException("Invalid port " + port);  
        return (T )Proxy.newProxyInstance(
        		interfaceClass.getClassLoader(),
        		new Class<?>[] {interfaceClass},
        		new InvocationHandler() {  
		            public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
		                Socket socket = new Socket(host, port);
		                try {
		                    ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
		                    try {
		                        output.writeUTF(method.getName());
		                        output.writeObject(method.getParameterTypes());
		                        output.writeObject(arguments);
		                        ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
		                        try {
		                            Object result = input.readObject();  
		                            if (result instanceof Throwable) {  
		                                throw (Throwable )result;  
		                            }  
		                            return result;  
		                        } finally {  
		                            input.close();  
		                        }  
		                    } finally {  
		                        output.close();  
		                    }  
		                } finally {  
		                    socket.close();  
		                }  
		            }  
        		}
        );  
    }
    
    private static void reflectAnalyzeAndCacheInfo(Class<?> clazz, int port) {
    	HashMap<String, Method> clazzMethodInfoItem = new HashMap<String, Method>();
    	Method[] methods = clazz.getMethods();
    	for(Method method:methods) {
    		String methodName = method.getName();
    		String methodSignature = getMethodSignature(method.getParameterTypes());
    		clazzMethodInfoItem.put(methodName +methodSignature, method);
    	}
    	//ExportEPCServiceClassInfo.put(clazz, clazzMethodInfoItem);
    	ExportEPCServiceClassInfo.put(port, clazzMethodInfoItem);
    }
    
    private final static String getMethodSignature(Class<?>[] parameterTypes) {
    		if(null==parameterTypes || 0==parameterTypes.length)
    			return "";
    		StringBuffer sb = new StringBuffer();
    		sb.append('(');
    		for(Class<?> paramClazz:parameterTypes) {
    			sb.append(paramClazz.getName());
    			sb.append(", ");
    		}
    		sb.append(')');
    		return sb.toString();
    }
    
    private final static Map<Object, Map<String, Method>> ExportEPCServiceClassInfo = new HashMap<Object, Map<String, Method>>();
}  