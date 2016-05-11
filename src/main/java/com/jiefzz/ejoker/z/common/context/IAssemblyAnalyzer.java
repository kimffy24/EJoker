package com.jiefzz.ejoker.z.common.context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public interface IAssemblyAnalyzer {

	/**
	 * return the dependence map
	 * @return
	 */
	public Map<String, Map<String, Field>> getDependenceMapper();
	
	/**
	 * return the initialize method map
	 * @return
	 */
	public Map<String, Set<Method>> getInitializeMapper();
	
	/**
	 * return @EService mapper
	 * @return
	 */
	public Set<Class<?>> getEServiceMapper();
	
}
