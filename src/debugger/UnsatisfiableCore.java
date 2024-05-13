package debugger;

import java.util.ArrayList;

public class UnsatisfiableCore {

	private ArrayList<String> rules;
	private ArrayList<String> explanations;
	private ArrayList<Integer> lines;
	
	public UnsatisfiableCore() {
		rules = new ArrayList<String>();
		explanations = new ArrayList<String>();
		lines = new ArrayList<Integer>();
	}
	
	public void addRule(String rule, String explanation, Integer line) {
		rules.add(rule);
		explanations.add(explanation);
		lines.add(line);
	}
	
	@Override
	public String toString() {
		return rules.toString() + " " + explanations.toString() + " " + lines.toString();
	}
	
	public ArrayList<Integer> getLines() {
		return lines;
	}
	
	public ArrayList<String> getRules() {
		return rules;
	}
	
	public ArrayList<String> getExplanations() {
		return rules;
	}
}