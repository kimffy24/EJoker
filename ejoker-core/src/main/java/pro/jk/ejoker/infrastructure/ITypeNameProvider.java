package pro.jk.ejoker.infrastructure;

import java.util.Map;

public interface ITypeNameProvider {

	public Class<?> getType(String typeName);
	
	public String getTypeName(Class<?> clazz);
	
	/**
	 * 设定一些Class和typeName的直接映射关系<br /><br />
	 * 
	 * 可以设置多组alias，但在其声明周期内无法撤销，且alias的对应关系不能重复和冲突<br /><br />
	 * 
	 * important!!! 使用Alias需要保证向后兼容<br />
	 * 即以前已经落盘的消息能被以后的每一个版本的ITypeNameProvider对象正确解析<br />
	 * @param dict
	 */
	public void applyAlias(Map<Class<?>, String> dict);
	
	/**
	 * 设定类型限定名到简名的修饰器<br /><br />
	 * 
	 * 仅可使用一个修饰器，在其声明周期内无法撤销<br /><br />
	 * 
	 * important!!! 使用类名修饰器需要保证向后兼容<br />
	 * 即以前已经落盘的消息能被以后的每一个版本的ITypeNameProvider对象正确解析<br />
	 * @param decorator
	 */
	public void useDecorator(IDecorator decorator);
	
	/**
	 * 提供类型的简名和全限定类名的映射关系<br />
	 * io传输中使用全限定类名有大量重复的包路径信息，可以在这里提供映射<br />
	 * 
	 * @author kimffy
	 *
	 */
	public static interface IDecorator {
		
		/**
		 * 给定简名，返回全限定类名
		 * @param typeName
		 * @return
		 */
		public String preGetType(String typeName);
		
		/**
		 * 给定全限定类名，返回简名
		 * @param typeName
		 * @return
		 */
		public String postGetTypeName(String typeName);
		
	}
}
