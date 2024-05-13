package debugger;

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
	
	public int getValue() {
		return value;
	}
	
	public String getAtom() {
		return atom;
	}
	
	public void setValue(int value) {
//		if(this.value != NOT_SET)
//			return;
//		if(!check(value))
//			return;
//		this.value = value;
		this.value = value;
	}
	
	private boolean check(int value) {
		if(value != FALSE && value != TRUE && value != UNDEFINED)
			return false;
		return true;
	}
}
