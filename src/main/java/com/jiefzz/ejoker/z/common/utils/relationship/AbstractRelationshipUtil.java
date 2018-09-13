package com.jiefzz.ejoker.z.common.utils.relationship;

public abstract class AbstractRelationshipUtil {

	protected final SpecialTypeCodecStore<?> specialTypeCodecStore;
	
	protected AbstractRelationshipUtil(SpecialTypeCodecStore<?> specialTypeCodecStore) {
		this.specialTypeCodecStore = specialTypeCodecStore;
	}
	
	protected Object processWithUserSpecialCodec(Object value, Class<?> fieldType) {
		if(null == specialTypeCodecStore)
			return null;
		
		SpecialTypeCodec fieldTypeCodec = specialTypeCodecStore.getCodec(fieldType);
		if(null == fieldTypeCodec)
			return null;
		
		/// 完全类型对等
		if(fieldType.equals(value.getClass()))
			return fieldTypeCodec.encode(value);
		
		return null;
	}
	
}
