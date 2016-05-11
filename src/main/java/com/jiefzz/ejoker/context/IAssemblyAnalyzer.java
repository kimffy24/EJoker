package com.jiefzz.ejoker.context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public interface IAssemblyAnalyzer {

	public Map<String, Map<String, Field>> getDependenceMapper();
	public Map<String, Set<Method>> getInitializeMapper();
	public Set<Class<?>> getEServiceMapper();
	
}
