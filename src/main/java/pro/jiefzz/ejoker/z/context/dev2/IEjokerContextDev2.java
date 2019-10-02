package pro.jiefzz.ejoker.z.context.dev2;

import pro.jiefzz.ejoker.z.system.functional.IVoidFunction;

public interface IEjokerContextDev2 extends IEJokerSimpleContext, IEJokerClazzScanner {

	public void refresh();
	
	public void discard();

	public void destroyRegister(IVoidFunction vf, int priority);
	
	default public void destroyRegister(IVoidFunction vf) {
		destroyRegister(vf, 50);
	}
	
	/**
	 * 浅注册： 仅仅注入 当前表现态 的类型 与 传入对象的 对应关系
	 * @param instance
	 */
	public void shallowRegister(Object instance);
	
}
