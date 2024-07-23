package application.model.debugger;

public class Response {

	private String rule;
	private int type;
	
	public Response(String rule, int type) {
		this.rule = rule;
		this.type = type;
	}
	
	@Override
	public String toString() {
		return rule;
	}
	
	public int getValue() {
		return type;
	}
	
	public String getRule() {
		return rule;
	}
		
}
