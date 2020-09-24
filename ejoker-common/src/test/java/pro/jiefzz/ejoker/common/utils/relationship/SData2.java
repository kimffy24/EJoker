package pro.jiefzz.ejoker.common.utils.relationship;

public class SData2 {

	private Double d1;
	
	private Float f1;
	
	private Long l1;
	
	private Byte b1;
	
	private Integer i1;
	
	private Short s1;

	public SData2(Double d1, Float f1, Long l1, Byte b1, Integer i1, Short s1) {
		this.d1 = d1;
		this.f1 = f1;
		this.l1 = l1;
		this.b1 = b1;
		this.i1 = i1;
		this.s1 = s1;
	}
	
	public SData2() {}

	public Double getD1() {
		return d1;
	}

	public Float getF1() {
		return f1;
	}

	public Long getL1() {
		return l1;
	}

	public Byte getB1() {
		return b1;
	}

	public Integer getI1() {
		return i1;
	}

	public Short getS1() {
		return s1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SData2 [");
		if(null != d1)
			sb.append("d1=").append(d1).append(", ");
		if(null != f1)
			sb.append("f1=").append(f1).append(", ");
		if(null != l1)
			sb.append("l1=").append(l1).append(", ");
		if(null != b1)
			sb.append("b1=").append(b1).append(", ");
		if(null != i1)
			sb.append("i1=").append(i1).append(", ");
		if(null != s1)
			sb.append("s1=").append(s1).append(", ");
		sb.append("]");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((b1 == null) ? 0 : b1.hashCode());
		result = prime * result + ((d1 == null) ? 0 : d1.hashCode());
		result = prime * result + ((f1 == null) ? 0 : f1.hashCode());
		result = prime * result + ((i1 == null) ? 0 : i1.hashCode());
		result = prime * result + ((l1 == null) ? 0 : l1.hashCode());
		result = prime * result + ((s1 == null) ? 0 : s1.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SData2 other = (SData2) obj;
		if (b1 == null) {
			if (other.b1 != null)
				return false;
		} else if (!b1.equals(other.b1))
			return false;
		if (d1 == null) {
			if (other.d1 != null)
				return false;
		} else if (!d1.equals(other.d1))
			return false;
		if (f1 == null) {
			if (other.f1 != null)
				return false;
		} else if (!f1.equals(other.f1))
			return false;
		if (i1 == null) {
			if (other.i1 != null)
				return false;
		} else if (!i1.equals(other.i1))
			return false;
		if (l1 == null) {
			if (other.l1 != null)
				return false;
		} else if (!l1.equals(other.l1))
			return false;
		if (s1 == null) {
			if (other.s1 != null)
				return false;
		} else if (!s1.equals(other.s1))
			return false;
		return true;
	};
	
}
