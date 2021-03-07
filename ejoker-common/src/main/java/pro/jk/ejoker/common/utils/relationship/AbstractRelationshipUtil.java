package pro.jk.ejoker.common.utils.relationship;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import pro.jk.ejoker.common.context.annotation.persistent.PersistentIgnore;
import pro.jk.ejoker.common.system.helper.Ensure;
import pro.jk.ejoker.common.utils.SerializableCheckerUtil;

public abstract class AbstractRelationshipUtil<KVP, VP> {

//	protected ThreadLocal<Queue<IVoidFunction>> taskQueueBox = ThreadLocal.withInitial(LinkedBlockingQueue::new);
	
	protected final IRelationshipScalpel<KVP, VP> eval;

	protected final SpecialTypeCodecStore<?> specialTypeCodecStore;
	
	protected AbstractRelationshipUtil(IRelationshipScalpel<KVP, VP> eval, SpecialTypeCodecStore<?> specialTypeCodecStore) {
		Ensure.notNull(eval, this.getClass().getSimpleName()+ ".eval");
		this.eval = eval;
		this.specialTypeCodecStore = specialTypeCodecStore;
	}
	
	protected Object processWithUserSpecialCodec(Object value, Class<?> fieldType) {
		if(null == specialTypeCodecStore)
			return null;
		
		SpecialTypeCodec fieldTypeCodec = specialTypeCodecStore.getCodec(fieldType);
		if(null == fieldTypeCodec)
			return null;
		
		/// 完全类型对等
		if(fieldType.equals(value.getClass()) || SerializableCheckerUtil.isPrimitiveTypeEqual(fieldType, value.getClass()))
			return fieldTypeCodec.encode(value);
		
		return null;
	}
	
	protected SpecialTypeCodec getDeserializeCodec(Class<?> fieldType) {
		if(null == specialTypeCodecStore)
			return null;
		
		return specialTypeCodecStore.getCodec(fieldType);
	}

	public static boolean checkIgnoreField(Field field) {
		int modifiers = field.getModifiers();
		return (
				field.isAnnotationPresent(PersistentIgnore.class)
				|| Modifier.isStatic(modifiers)
				|| Modifier.isFinal(modifiers)
		);
	}
}
