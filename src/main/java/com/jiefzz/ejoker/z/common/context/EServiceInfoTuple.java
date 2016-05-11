package com.jiefzz.ejoker.z.common.context;

import java.util.HashSet;
import java.util.Set;

import com.jiefzz.ejoker.z.common.UnimplementException;

public class EServiceInfoTuple {
	public final String id;							//供用户指明id引用
	public final Class<?> eServiceClassType;		//实现类
	public final boolean force;						//强制位，让其排在链表的首位
	private EServiceInfoTuple next = null;			//链表的下一个指针
	private Set<Class<?>> listContainSet = new HashSet<Class<?>>();
													//链表中已存在的实现

	public EServiceInfoTuple(Class<?>eServiceClassType, String id, boolean force){
		this.eServiceClassType = eServiceClassType;
		this.id = id;
		this.force = force;
		listContainSet.add(eServiceClassType);
	}
	public EServiceInfoTuple(Class<?>eServiceClassType, String id){ this(eServiceClassType, id, false); }
	public EServiceInfoTuple(Class<?>eServiceClassType, boolean force){ this(eServiceClassType, eServiceClassType.getName()); }
	public EServiceInfoTuple(Class<?>eServiceClassType){ this(eServiceClassType, eServiceClassType.getName()); }
	public EServiceInfoTuple add(EServiceInfoTuple esti){
		if(listContainSet.contains(esti)) return this;
		esti.listContainSet = this.listContainSet;
		if(esti.force!=true) {
			if(next==null) next = esti;
			else next.add(esti);
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