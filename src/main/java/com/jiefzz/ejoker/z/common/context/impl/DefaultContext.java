package com.jiefzz.ejoker.z.common.context.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.jiefzz.ejoker.z.common.UnimplementException;
import com.jiefzz.ejoker.z.common.context.AbstractContext;
import com.jiefzz.ejoker.z.common.context.ContextRuntimeException;
import com.jiefzz.ejoker.z.common.context.EServiceInfoTuple;
import com.jiefzz.ejoker.z.common.context.IAssemblyAnalyzer;
import com.jiefzz.ejoker.z.common.context.IInstanceBuilder;
import com.jiefzz.ejoker.z.common.context.LazyInjectTuple;

public class DefaultContext extends AbstractContext {

	private Lock lock = new ReentrantLock();

	public DefaultContext(){ super(); }

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

	/**
	 * use Concurrent.lock rather than the synchronized keyword 
	 */
	public <TInstance> TInstance get(Class<TInstance> clazz){
		lock.lock();
		Object instance = getInstance(clazz);
		if (instance != null) return (TInstance ) instance;
		instance = innerGet(clazz);
		lock.unlock();
		if ( getMultiDependenceInstanceMapper().size()!=0 )
			throw new ContextRuntimeException("There some worng dependence could not resolve!!!");
		return (TInstance ) instance;
	}

	public <TInstance> void set(Class<TInstance> clazz, TInstance instance) {
		set(clazz.getName(), instance);
	}

	public <TInstance> void set(String instanceType, TInstance instance) {
		adoptInstance(instanceType, instance);
	}

	@Override
	public Class<?> resolve(String interfaceName){
		try {
			return resolve(Class.forName(interfaceName));
		} catch (ClassNotFoundException e) {
			throw new ContextRuntimeException("This Exception will never occur, please send a report to constructor!!!", e);
		}
	}

	@Override
	public Class<?> resolve(Class<?> interfaceType){
		if (eServiceInterfaceMapper.containsKey(interfaceType)) {
			return eServiceInterfaceMapper.get(interfaceType).eServiceClassType;
		}
		return interfaceType;
	}

	private <TInstance> TInstance innerGet(Class<TInstance> clazz){
		Class<?> clazzImpl = resolve(clazz);
		if(clazzImpl.isInterface())
			throw new ContextRuntimeException(String.format("Could not found ImplementClass for [%s]", clazz.getName()));
		String clazzName = clazzImpl.getName();
		IAssemblyAnalyzer assemblyInfo = getAssemblyInfo(clazzName);
		IInstanceBuilder instanceBuilder = new InstanceBuilderImpl(
				this,
				clazzImpl,
				assemblyInfo.getDependenceMapper().get(clazzName));
		TInstance instance = (TInstance) instanceBuilder.doCreate();
		loadAllWating();
		return instance;
	}

	private void loadAllWating(){
		Map<Class<?>, List<LazyInjectTuple>> multiDependenceInstance = getMultiDependenceInstanceMapper();
		while (multiDependenceInstance.size()!=0) {
			Set<Class<?>> waitingObjectInstances = multiDependenceInstance.keySet();
			Class<?> nextResolvObjectType = waitingObjectInstances.iterator().next();
			try {
				innerGet(nextResolvObjectType);
			} catch (Exception e) {
				throw new ContextRuntimeException("Could not resolved dependence!!!", e);
			}
		}
	}

	/**
	 * get e-joker context scan information.
	 * @param classFullName
	 * @return
	 */
	private IAssemblyAnalyzer getAssemblyInfo(String classFullName){
		IAssemblyAnalyzer assemblyAnalyzer = null;
		Set<Entry<String, IAssemblyAnalyzer>> entrySet = assemblyMapper.entrySet();
		for(Entry<String, IAssemblyAnalyzer> entry : entrySet)
			if(classFullName.startsWith(entry.getKey()))
				return assemblyAnalyzer = entry.getValue();
		if(assemblyAnalyzer==null)
			throw new ContextRuntimeException("AssemblyInfo for ["+classFullName+"] is not found!!!Did you forget make it into to scan?");
		return assemblyAnalyzer;
	}

	private void combineEServiceInterfaceMapper(Set<Class<?>> eServiceClasses){
		for (Class<?> clazz : eServiceClasses) {
			Class<?>[] implementInterfaces = clazz.getInterfaces();
			for (Class<?> intf : implementInterfaces) {
				EServiceInfoTuple eServiceTupleInfo = eServiceInterfaceMapper.get(intf);
				if(eServiceTupleInfo==null)
					eServiceTupleInfo = new EServiceInfoTuple(clazz);
				else
					eServiceTupleInfo = eServiceTupleInfo.add(new EServiceInfoTuple(clazz));
				eServiceInterfaceMapper.put(intf, eServiceTupleInfo);
			}
		}
	}

	private final Map<String, IAssemblyAnalyzer> assemblyMapper = new HashMap<String, IAssemblyAnalyzer>();
	private final Map<Class<?>, EServiceInfoTuple> eServiceInterfaceMapper = new HashMap<Class<?>, EServiceInfoTuple>();
	
	
}
