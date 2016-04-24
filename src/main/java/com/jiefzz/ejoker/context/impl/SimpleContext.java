package com.jiefzz.ejoker.context.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.jiefzz.ejoker.context.AbstractContext;
import com.jiefzz.ejoker.context.ContextRuntimeException;
import com.jiefzz.ejoker.context.IAssemblyAnalyzer;
import com.jiefzz.ejoker.context.IInstanceBuilder;
import com.jiefzz.ejoker.extension.infrastructure.IJSONConverter;
import com.jiefzz.ejoker.extension.infrastructure.impl.JSONConverterUseJsonSmartImpl;

public class SimpleContext extends AbstractContext {

	public static void main(String[] args) {
		//		String packageName = "com.jiefzz.ejoker.";
		//		System.out.println(packageName.lastIndexOf('.'));
		//		System.out.println(packageName.length());
		//		System.out.println(packageName.startsWith("com.jiefzz.ejoker.13412341234"));

		IJSONConverter jsonConverter = new JSONConverterUseJsonSmartImpl();

		SimpleContext simpleContext = new SimpleContext();
		simpleContext.annotationScan("com.jiefzz.ejoker");

		Set<String> keySet = simpleContext.assemblyMapper.keySet();
		for ( String key : keySet ){
			IAssemblyAnalyzer aa = simpleContext.assemblyMapper.get(key);
			System.out.println(jsonConverter.convert(aa));
		}

	}

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
		assemblyMapper.put(specificPackage, aa);
	}
	
	private <T> T get(Class<T> clazz){
		IAssemblyAnalyzer assemblyInfo = getAssemblyInfo(clazz.getName());
		IInstanceBuilder instanceBuilder = new InstanceBuilderImpl(this);
		return null;
	}

	private IAssemblyAnalyzer getAssemblyInfo(String classFullName){
		Set<String> keySet = assemblyMapper.keySet();
		for ( String key : keySet ){
			if ( classFullName.startsWith(key)) return assemblyMapper.get(key);
		}
		throw new ContextRuntimeException("AssemblyInfo for ["+classFullName+"] is not found!!!Did you forget make it into to scan?");
	}

	private final Map<String, IAssemblyAnalyzer> assemblyMapper = new HashMap<String, IAssemblyAnalyzer>();
}
