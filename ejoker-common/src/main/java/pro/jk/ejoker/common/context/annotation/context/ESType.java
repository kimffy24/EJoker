package pro.jk.ejoker.common.context.annotation.context;

public enum ESType {

	/**
	 * 一个普通的EJoker内部的Service对象，默认值
	 */
	NORMAL,
	
	/**
	 * 
	 * Tell the configureObject or contextObject to scan its handler method<br />
	 * 用于提供命令响应的handler对象<br /><br />
	 * 
	 * 框架会扫描当前对象中的所有handle/handleAsync方法
	 */
	COMMAND_HANDLER,
	
	/**
	 * Tell the configureObject or contextObject to scan its handler method<br />
	 * 用于提供消息响应的handler对象<br /><br />
	 * 
	 * 框架会扫描当前对象中的所有handle/handleAsync方法
	 */
	MESSAGE_HANDLER,
	
	;
	
}
