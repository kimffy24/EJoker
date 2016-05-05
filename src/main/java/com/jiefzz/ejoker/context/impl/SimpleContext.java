package com.jiefzz.ejoker.context.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.context.AbstractContext;
import com.jiefzz.ejoker.context.ContextRuntimeException;
import com.jiefzz.ejoker.context.IAssemblyAnalyzer;
import com.jiefzz.ejoker.context.IInstanceBuilder;
import com.jiefzz.ejoker.context.LazyInjectTuple;

public class SimpleContext extends AbstractContext {

	public SimpleContext(){ super(); }

	public void annotationScan(String specificPackage) {
		if ( specificPackage.lastIndexOf('.') == (specificPackage.length()-1) ) 
			specificPackage = specificPackage.substring(0, specificPackage.length()-1);
		Set<String> keySet = assemblyMapper.keySet();
		for ( String key : keySet ){
			// 传入的包是某个已经被分析的包的子包或就是已存在的包，则不再分析
			if(specificPackage.startsWith(key)) return;
			// 传入的包包含了已被分析过的包，则先去除这个被分析过的子包
			if(key.startsWith(specificPackage)) assemblyMapper.remove(key);
		}
		IAssemblyAnalyzer aa = new AssemblyAnalyzerImpl(specificPackage);
		combineEServiceInterfaceMapper(aa.getEServiceMapper());
		assemblyMapper.put(specificPackage, aa);
	}

	public synchronized <TInstance> TInstance get(Class<TInstance> clazz){
		Object instance = getInstance(clazz);
		if (instance != null) return (TInstance ) instance;
		instance = innerGet(clazz);
		if ( getMultiDependenceInstanceMapper().size()!=0 )
			throw new ContextRuntimeException("There some worng dependence could not resolve!!!");
		return (TInstance ) instance;
	}
	
	public synchronized <TInstance> void set(Class<TInstance> clazz, TInstance instance) {
		set(clazz.getName(), instance);
	}
	
	public synchronized <TInstance> void set(String instanceType, TInstance instance) {
		adoptInstance(instanceType, instance);
	}
	

	@Override
	public String resolve(String interfaceName){
		if (eServiceInterfaceMapper.containsKey(interfaceName))
			return eServiceInterfaceMapper.get(interfaceName);
		return interfaceName;
	}

	private <TInstance> TInstance innerGet(Class<TInstance> clazz){

		Class<?> clazzImpl = clazz;
		if (clazz.isInterface()) {
			String clazzImplName = resolve(clazz.getName());
			try {
				clazzImpl = Class.forName(clazzImplName);
			} catch (Exception e) {
				System.err.println("fetch "+clazzImplName);
				throw new ContextRuntimeException("This Exception will never occur, please send a report to constructor!!!", e);
			}
		}
		String clazzName = clazzImpl.getName();
		IAssemblyAnalyzer assemblyInfo = getAssemblyInfo(clazzName);
		IInstanceBuilder instanceBuilder = new InstanceBuilderImpl(
				this,
				clazzImpl,
				assemblyInfo.getDependenceMapper().get(clazzName),
				assemblyInfo.getInitializeMapper().get(clazzName));
		TInstance instance = (TInstance) instanceBuilder.doCreate();
		loadAllWating();
		return instance;
	}
	
	private void loadAllWating(){
		Map<String, List<LazyInjectTuple>> multiDependenceInstance = getMultiDependenceInstanceMapper();
		while (multiDependenceInstance.size()!=0) {
			Set<String> waitingObjectInstances = multiDependenceInstance.keySet();
			String nextResolvObjectType = waitingObjectInstances.iterator().next();
			try {
				innerGet(Class.forName(nextResolvObjectType));
			} catch (Exception e) {
				throw new ContextRuntimeException("This Exception will never occur, please send a report to constructor!!!", e);
			}
		}
//		Set<String> waitingObjectInstances = multiDependenceInstance.keySet();
//		List<String> waitintList = new ArrayList<String>();
//		waitintList.addAll(waitingObjectInstances);
//		for (String watingObjectInstance : waitingObjectInstances )
//			try {
//				innerGet(Class.forName(watingObjectInstance));
//			} catch (Exception e) {
//				throw new ContextRuntimeException("This Exception will never occur, please send a report to constructor!!!", e);
//			}
	}

	private IAssemblyAnalyzer getAssemblyInfo(String classFullName){
		Set<String> keySet = assemblyMapper.keySet();
		for ( String key : keySet ){
			if ( classFullName.startsWith(key)) return assemblyMapper.get(key);
		}
		throw new ContextRuntimeException("AssemblyInfo for ["+classFullName+"] is not found!!!Did you forget make it into to scan?");
	}

	private void combineEServiceInterfaceMapper(Set<String> eServiceClasses){
		for (String clazzName : eServiceClasses) {
			Class<?> clazz;
			try {
				clazz = Class.forName(clazzName);
			} catch (Exception e) {
				throw new ContextRuntimeException("This Exception will never occur, please send a report to constructor!!!", e);
			}
			Class<?>[] implementInterfaces = clazz.getInterfaces();
			for (Class<?> intf : implementInterfaces) {
				String interfaceName = intf.getName();
				if ( eServiceInterfaceMapper.containsKey(interfaceName) )
					throw new ContextRuntimeException("The interface ["+interfaceName+"] has regist an implemented class!!!");
				eServiceInterfaceMapper.put(interfaceName, clazzName);
			}
		}
	}

	private final Map<String, IAssemblyAnalyzer> assemblyMapper = new HashMap<String, IAssemblyAnalyzer>();
	private final Map<String, String> eServiceInterfaceMapper = new HashMap<String, String>();
}
