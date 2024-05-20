package debugger;

public class Response {

	private String rule;
	private int type;
	
	public Response(String rule, int type) {
		this.rule = rule;
		this.type = type;
	}
	
	public int getValue() {
		return type;
	}
	
	public String getRule() {
		return rule;
	}
		
}
