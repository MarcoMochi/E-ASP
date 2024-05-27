package application.model.debugger;

import java.util.Objects;

public class QueryAtom {
	
	public final static int FALSE = 0;
	public final static int TRUE = 1;
	public final static int UNDEFINED = 2;
	public final static int NOT_SET = 3;
	
	private String atom;
	private int value;
	
	public QueryAtom(String atom, int value) {
		this.atom = atom;
		if(check(value))
			this.value = value;
		else
			this.value = NOT_SET;			
	}
	
	public String toText(Debugger d) {
		if (d.getDerivedAtoms().contains(getAtom()))
			return getAtom();
		else
			return "not " + getAtom();
	}
	
	public int getValue() {
		return value;
	}
	
	public String getAtom() {
		return atom;
	}
	
	public void setValue(int value) {
		this.value = value;
	}
	
	private boolean check(int value) {
		if(value != FALSE && value != TRUE && value != UNDEFINED)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		if (getValue() == 0)
			return "not " + getAtom();
		else
			return getAtom();
	}
	

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		QueryAtom queryAtom = (QueryAtom) o;
		return value == queryAtom.value && Objects.equals(atom, queryAtom.atom);
	}

	@Override
	public int hashCode() {
		return Objects.hash(atom, value);
	}
}
