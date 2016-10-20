package com.jiefzz.ejoker.z.common.remoting;

public class RemotingRequest extends RemotingMessage {

	public RemotingRequest(short code, byte[] body, long sequence) {
		super(code, body, sequence);
	}
	public RemotingRequest(short code, byte[] body) {
		this(code, body, 0);
	}

	public String toString() {
		return this.getClass().getName() +".toString() is unimplemented!";
	}
	
}
