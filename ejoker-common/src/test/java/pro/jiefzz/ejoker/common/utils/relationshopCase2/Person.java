package pro.jiefzz.ejoker.common.utils.relationshopCase2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pro.jk.ejoker.common.context.annotation.persistent.PersistentIgnore;

public class Person {

	final static Random random = new Random(1345655123);
	
	public int s;
	public final int sex;
	
	public int weight;
	public int salary;
	
	@PersistentIgnore
	public Person father = null;
	@PersistentIgnore
	public Person mother = null;
	public List<Person> children = new ArrayList<>();
	
	public Person(int sex) {
		this.sex = sex;
		s = sex;
		weight = ((Number )(random.nextInt(10000)%50+50)).intValue();
		salary = ((Number )(random.nextInt()%10000+10000)).intValue();
		
	}
	
	public void general(Person partner) {
		if(partner.sex == this.sex)
			return;
		Person child = new Person(((Number )(System.currentTimeMillis()%2)).intValue());
		if(this.sex == 1) {
			child.father = this;
			child.mother = partner;
			this.children.add(child);
		} else {
			child.father = partner;
			child.mother = this;
			partner.children.add(child);
		}
		
	}
	
	public void general(Person partner, int s) {
		if(partner.sex == this.sex)
			return;
		Person child = new Person(s);
		if(this.sex == 1) {
			child.father = this;
			child.mother = partner;
			this.children.add(child);
		} else {
			child.father = partner;
			child.mother = this;
			partner.children.add(child);
		}
		
	}
	
}
