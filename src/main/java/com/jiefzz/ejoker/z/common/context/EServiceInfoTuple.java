package com.jiefzz.ejoker.z.common.context;

import com.jiefzz.ejoker.z.common.UnimplementException;

public class EServiceInfoTuple {
	public final String id;
	public final Class<?> eServiceClassType;
	public final boolean force;
	private EServiceInfoTuple next = null;

	public EServiceInfoTuple(Class<?>eServiceClassType, String id, boolean force){
		this.eServiceClassType = eServiceClassType;
		this.id = id;
		this.force = force;
	}
	public EServiceInfoTuple(Class<?>eServiceClassType, String id){ this(eServiceClassType, id, false); }
	public EServiceInfoTuple(Class<?>eServiceClassType, boolean force){ this(eServiceClassType, eServiceClassType.getName()); }
	public EServiceInfoTuple(Class<?>eServiceClassType){ this(eServiceClassType, eServiceClassType.getName()); }
	public EServiceInfoTuple add(EServiceInfoTuple esti){
		if(esti.force!=true) {
			next.add(esti);
			return this;
		} else {
			if(force==true) throw new ContextRuntimeException("More than one implements!!! ["+eServiceClassType.getName()+"] could not become a default implement.");
			esti.next = this;
			return esti;
		} 

	}
	public EServiceInfoTuple getWithId(String id) {
		throw new UnimplementException(EServiceInfoTuple.class.getName()+".getWithId(String id)");
	}
}