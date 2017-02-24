package com.jiefzz.ejoker.eventing;

public enum EventAppendResult {

	/**
	 * 无定义。
	 */
	Undefined,

	/**
	 * 成功
	 */
	Success,

	/**
	 * 失败
	 */
	Failed,

	/**
	 * 重复事件
	 */
	DuplicateEvent,

	/**
	 * 重复命令
	 */
	DuplicateCommand

}
