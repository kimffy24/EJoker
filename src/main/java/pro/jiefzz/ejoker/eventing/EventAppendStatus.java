package pro.jiefzz.ejoker.eventing;

public enum EventAppendStatus {
	
	/**
	 * 无定义。
	 */
	Undefined,

	/**
	 * 成功
	 */
	Success,

	/**
	 * 重复事件
	 */
	DuplicateEvent,

	/**
	 * 重复命令
	 */
	DuplicateCommand
	
}
