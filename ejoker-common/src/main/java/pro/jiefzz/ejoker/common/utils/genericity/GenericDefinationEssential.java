package pro.jiefzz.ejoker.common.utils.genericity;

/**
 * 
 * Represents a object must be work with a {@link GenericDefination} object.<br /><br />
 * 
 * 继承此基类标明子类必须在某个{@link GenericDefination}下才有意义<br /><br />
 * 
 * @author kimffy
 *
 */
public abstract class GenericDefinationEssential {
	
	protected final GenericDefination referDefination;
	
	protected GenericDefinationEssential(GenericDefination referDefination) {
		this.referDefination = referDefination;
	}
	
	public GenericDefination getGenericDefination() {
		return referDefination;
	}
	
}
