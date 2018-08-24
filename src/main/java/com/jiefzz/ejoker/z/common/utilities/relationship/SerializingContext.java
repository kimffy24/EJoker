package com.jiefzz.ejoker.z.common.utilities.relationship;


public class SerializingContext {
	
	private LevelNode start = null, end = null;
	
	public SerializingContext() {}
	
	public SerializingContext process(String name) {
		internalProcess(new KVLevelNode(name));
		return this;
	}
	
	public SerializingContext process(int index) {
		internalProcess(new VLevelNode(index));
		return this;
	}

	public SerializingContext shot() {
		LevelNode newEnd = end.prev;
		newEnd.next = null;
		this.end = newEnd;
		return this;
	}
	
	public String getCoordinate() {
		LevelNode current = start;
		StringBuilder sb = new StringBuilder();
		while(null != current) {
			sb.append(current.getCoordinate());
			current = current.next;
		}
		sb.delete(0, 1);
		return sb.toString();
	}
	
	private void internalProcess(LevelNode node) {
		if(end == null) {
			start = end = node;
		} else {
			end.next = node;
			node.prev = end;
			end = node;
		}
	}

	private static abstract class LevelNode {
		
		LevelNode prev = null;
		LevelNode next = null;
		
		public abstract String getCoordinate();
		
	}

	private static class KVLevelNode extends LevelNode {
		
		public final String name;
		
		public KVLevelNode(String name) {
			this.name = name;
		}

		@Override
		public String getCoordinate() {
			StringBuilder sb = new StringBuilder();
			sb.append('.').append(name);
			return sb.toString();
		}
		
	}

	private static class VLevelNode extends LevelNode {
		
		public final int index;
		
		public VLevelNode(int index) {
			this.index = index;
		}

		@Override
		public String getCoordinate() {
			StringBuilder sb = new StringBuilder();
			sb.append('[').append(index).append(']');
			return sb.toString();
		}
		
	}
}
