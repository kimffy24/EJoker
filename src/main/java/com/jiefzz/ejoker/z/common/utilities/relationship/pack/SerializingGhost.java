package com.jiefzz.ejoker.z.common.utilities.relationship.pack;

public class SerializingGhost {
	
	private String tg;
	
	private String ot;
	
	public SerializingGhost(String typeSign, Class<?> clazz) {
		tg = typeSign;
		ot = clazz.getName();
	}
	
	public String getTg() {
		return tg;
	}
	
	public String getOt() {
		return ot;
	}
	
}
