package application.model.debugger;

import java.util.ArrayList;

public class UnsatisfiableCore {

	private ArrayList<Response> rules;
	
	public UnsatisfiableCore() {
		rules = new ArrayList<Response>();
	}
	
	public void addRule(String rule, Integer type) {
		rules.add(new Response(rule, type));
	}
	
	@Override
	public String toString() {
		return rules.toString();
	}
	
	
	public ArrayList<Response> getRules() {
		return rules;
	}
	
}