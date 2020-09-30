package pro.jiefzz.ejoker.common.utils.relationship.pro;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pro.jiefzz.ejoker.common.utils.relationship.pro.JSONCuRuProTest.Favior;
import pro.jk.ejoker.common.context.annotation.persistent.PersistentIgnore;
import pro.jk.ejoker.common.service.IJSONStringConverterPro;
import pro.jk.ejoker.common.service.impl.JSONStringConverterProUseJsonSmartImpl;
import pro.jk.ejoker.common.utils.genericity.TypeRefer;

public class ExpressionTest3 {

	private final static IJSONStringConverterPro pro = new JSONStringConverterProUseJsonSmartImpl();
	
	public final static String tStr = "{\"set4T\":{\"place\":[[{\"name\":\"九寨沟\",\"address\":\"四川\"}],[{\"name\":\"雪乡\",\"address\":\"黑龙江\"}]],\"soft\":[[{\"name\":\"宝宝巴士\",\"address\":\"coco's iphone\"},{\"name\":\"邻居家的大龙猫\",\"address\":\"mac:8002\"}]]}}";

	static {
		System.err.println(tStr);
	}
	
	public final static class CBX<T> {
		private Map<String, T[][]> set4T = null;
		public Map<String, T[][]> getSet4T() {
			return set4T;
		}
		@Override
		public String toString() {
			return this.getClass().getSimpleName()+pro.convert(this, type);
		}
		@PersistentIgnore
		private  TypeRefer<CBX<T>> type = null;
		public CBX<T> markType(TypeRefer<CBX<T>> type) {
			this.type = type;
			return this;
		}
	}
	@Test
	public void test1() {
		TypeRefer<CBX<Favior>> typeRefer = new TypeRefer<CBX<Favior>>() {};
		CBX<Favior> revert = pro.revert(tStr, typeRefer);
		System.err.println(revert.markType(typeRefer));
	}
	
	public final static class CBY<T> {
		private Map<String, T[]> set4T = null;
		public Map<String, T[]> getSet4T() {
			return set4T;
		}
		@Override
		public String toString() {
			return this.getClass().getSimpleName()+pro.convert(this, type);
		}
		@PersistentIgnore
		private  TypeRefer<CBY<T>> type = null;
		public CBY<T> markType(TypeRefer<CBY<T>> type) {
			this.type = type;
			return this;
		}
	}
	@Test
	public void test2_0() {
		TypeRefer<CBY<Favior[]>> typeRefer = new TypeRefer<CBY<Favior[]>>() {};
		CBY<Favior[]> revert = pro.revert(tStr, typeRefer);
		System.err.println(revert.markType(typeRefer));
	}
	@Test
	public void test2_1() {
		TypeRefer<CBY<List<Favior>>> typeRefer = new TypeRefer<CBY<List<Favior>>>() {};
		CBY<List<Favior>> revert = pro.revert(tStr, typeRefer);
		System.err.println(revert.markType(typeRefer));
	}
	
	public final static class CBZ<T> {
		private Map<String, T> set4T = null;
		public Map<String, T> getSet4T() {
			return set4T;
		}
		@Override
		public String toString() {
			return this.getClass().getSimpleName()+pro.convert(this, type);
		}
		@PersistentIgnore
		private  TypeRefer<CBZ<T>> type = null;
		public CBZ<T> markType(TypeRefer<CBZ<T>> type) {
			this.type = type;
			return this;
		}
	}
	@Test
	public void test3_1() {
		TypeRefer<CBZ<Favior[][]>> typeRefer = new TypeRefer<CBZ<Favior[][]>>() {};
		CBZ<Favior[][]> revert = pro.revert(tStr, typeRefer);
		System.err.println(revert.markType(typeRefer));
	}
	@Test
	public void test3_2() {
		TypeRefer<CBZ<List<Favior[]>>> typeRefer = new TypeRefer<CBZ<List<Favior[]>>>() {};
		CBZ<List<Favior[]>> revert = pro.revert(tStr, typeRefer);
		System.err.println(revert.markType(typeRefer));
	}
	@Test
	public void test3_3() {
		TypeRefer<CBZ<List<List<Favior>>>> typeRefer = new TypeRefer<CBZ<List<List<Favior>>>>() {};
		CBZ<List<List<Favior>>> revert = pro.revert(tStr, typeRefer);
		System.err.println(revert.markType(typeRefer));
	}
	@Test
	public void test3_4() {
		TypeRefer<CBZ<List<Favior>[]>> typeRefer = new TypeRefer<CBZ<List<Favior>[]>>() {};
		CBZ<List<Favior>[]> revert = pro.revert(tStr, typeRefer);
		System.err.println(revert.markType(typeRefer));
	}
	@Test
	public void test3_5() {
		TypeRefer<CBZ<List<Set<Favior>>>> typeRefer = new TypeRefer<CBZ<List<Set<Favior>>>>() {};
		CBZ<List<Set<Favior>>> revert = pro.revert(tStr, typeRefer);
		System.err.println(revert.markType(typeRefer));
	}
	
}
