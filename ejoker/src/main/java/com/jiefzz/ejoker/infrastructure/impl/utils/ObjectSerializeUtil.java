package com.jiefzz.ejoker.infrastructure.impl.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.jiefzz.ejoker.utils.Base64Utils;

public class ObjectSerializeUtil {

	public final static String serialize(Serializable object){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return Base64Utils.encode(baos.toByteArray());
	}
	
	public final static Serializable deserialize(String encodeString) {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new ByteArrayInputStream(Base64Utils.decode(encodeString)));
			return (Serializable )ois.readObject();
		} catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}
	}
}
