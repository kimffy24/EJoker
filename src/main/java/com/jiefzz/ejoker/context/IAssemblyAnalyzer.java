package com.jiefzz.ejoker.context;

import java.util.Map;
import java.util.Set;

public interface IAssemblyAnalyzer {

	public Map<String, Set<String>> getDependenceMapper();
	public Map<String, Set<String>> getInitializeMapper();
	
}
