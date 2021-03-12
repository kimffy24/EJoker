package pro.jiefzz.ejoker.common.utils.relationshopCase2;

public class TestObject3 {

	public TColor[] colors = new TColor[]{TColor.RED, TColor.BLUE};
	
	public Object[] ta = new Object[3];
	
	public TestSingle[] tsa= new TestSingle[2];
	
	// public char tet = 't';
	
	public TestObject3() {
		ta[0] = "牛逼";
		
		//ta[1] = new TestSingle();
		
		ta[2] = TColor.YELLOW;
		
		tsa[0] = new TestSingle();
		tsa[0].a = 678;
		tsa[0].use = false;

		tsa[1] = new TestSingle();
		//tsa[1].c = '?';
	}
}
