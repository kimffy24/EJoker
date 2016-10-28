package com.jiefzz.ejoker.z.common.context;

public interface IEJokerClassMetaAnalyzer {

	/**
	 * 分析记录单个类的元数据信息
	 * @param clazz
	 */
	public void analyzeClassMeta(Class<?> clazz);
}
