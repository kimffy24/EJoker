package pro.jiefzz.ejoker.common.utils.relationshopCase2;

import java.util.ArrayList;
import java.util.List;

public class TestObject2 {

	public int x = 1;
	public double y = 0.5d;
	public float zz = 0.3125f;
	
	public long zzz = Long.MAX_VALUE;
	
	public short sd = 218;
	
	public String name = "龙金飞";
	
	public String[] address = new String[]{"广东", "广州"};

	public String[][] hobbies = new String[][]{{"篮球", "足球"}, {"计算机", "金融学"}};
	
	public TestObject3 t3 = new TestObject3();
	
	public List<TestDouble> testDoubles = new ArrayList<>();
	
	public TestObject2() {
		testDoubles.add(new TestDouble());
		TestDouble txx = new TestDouble();
		txx.dd = Math.E;
		testDoubles.add(txx);
	}
}
