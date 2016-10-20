package com.jiefzz.ejoker.z.common.remoting;

public class RemotingMessage {

	public byte[] body;
	public short code;
	public long sequence;
	public short type;
	
	public RemotingMessage(short code, byte[] body, long sequence) {
		this.code = code;
		this.body = body;
		this.sequence = sequence;
	}
}
