package com.jiefzz.ejoker.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * JVM提供的序列化工具类
 * @author jiefzz
 * @deprecated 如果真的需要，考虑google的protobuf实现
 */
@Deprecated
public final class JavaObjectSerializeUtil {

	/**
	 * java自带的序列化函数<br>
	 * Java对象转字节序列
	 * @param object
	 * @return
	 */
	public final static String serialize(Serializable object){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
		} catch (IOException e) {
			throw new RuntimeException(object.getClass().getName()+" serialize faild!!!", e);
		}
		return Base64Utils.encode(baos.toByteArray());
	}
	
	/**
	 * java自带的反序列化函数<br>
	 * 字节序列转Java对象
	 * @param encodeString
	 * @return
	 */
	public final static Serializable deserialize(String encodeString) {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new ByteArrayInputStream(Base64Utils.decode(encodeString)));
			return (Serializable )ois.readObject();
		} catch ( Exception e ) {
			throw new RuntimeException("Deserialize faild!!!", e);
		}
	}
}
