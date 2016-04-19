package com.jiefzz.ejoker.utils;

public class ObjectId {

	private org.bson.types.ObjectId innerObjectId = null;
	private ObjectId() {
		innerObjectId = new org.bson.types.ObjectId();
	}
	
	public String toHexString(){
		return innerObjectId.toHexString();
	}
	
	public String toDuotricemaryString() {
		try {
			throw new Exception("unimpelement");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static final String generateNewStringId(){
		return (new ObjectId()).toHexString();
	}
}
