package pro.jiefzz.ejoker.z.task;

public enum AsyncTaskStatus {
	
	/**
	 * 不占用0号定义
	 */
	Undefined,
	
	/**
	 * 执行成功
	 */
	Success,
	
	/**
	 * io异常
	 */
	IOException,
	
	/**
	 * 执行失败
	 */
	Failed
}
