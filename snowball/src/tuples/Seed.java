package tuples;

public class Seed {
	
	public String e1;
	public String e2;
	
	public Seed(String e1, String e2) {
		super();
		this.e1 = e1;
		this.e2 = e2;
	}
	
	public String toString(){
		return e1 + '\t' + e2;		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((e1 == null) ? 0 : e1.hashCode());
		result = prime * result + ((e2 == null) ? 0 : e2.hashCode());
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
		Seed other = (Seed) obj;
		if (e1 == null) {
			if (other.e1 != null)
				return false;
		} else if (!e1.equals(other.e1))
			return false;
		if (e2 == null) {
			if (other.e2 != null)
				return false;
		} else if (!e2.equals(other.e2))
			return false;
		return true;
	}

	public int compareTo(Seed s) {
		int value = 0;
		if (!e1.equalsIgnoreCase(s.e1)) value = this.e1.compareTo(s.e1);
		if (e1.equalsIgnoreCase(s.e1)) value = this.e2.compareTo(s.e2);
		return value;
	}
}
