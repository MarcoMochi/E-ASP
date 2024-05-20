package debugger;

import java.util.List;
import java.util.Map;

public class Justifier {
	private Boolean debug_rules;
	private Boolean debug_AS;
	private String program;
	
	
	public Justifier(String program, Boolean debug_rule, Boolean debug_AS) {
		this.program = program;
		this.debug_AS = debug_AS;
		this.debug_rules = debug_rule;
	} 
	
	public List<String> computeFirstAnswerSet() {
		return null;
	}
	
	// String più tipo: 0 - regola, 1 - fatto, 2 - atomo_to_explain, 3 - regola_con_aggregato
	public List<Response> justify(List<QueryAtom> chain, QueryAtom atom) {
		return null;
	}
	
	public List<Map<String, List<String>>> expandAggregate(String rule, List<String> answer_set) {
		return null;	
	}
	
	
}
