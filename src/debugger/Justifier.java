package debugger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Justifier {
	private String program;
	private Debugger d;
	private List<QueryAtom> qa;
	
	
	public Justifier(String program, Boolean debug_rules, Boolean debug_AS) {
		this.program = program;
		this.d = new Debugger(debug_rules, debug_AS);
	} 
	
	public List<QueryAtom> computeFirstAnswerSet() throws IOException {
		this.d.getFacts(this.program);
		this.d.computeAtoms(this.program);
		qa = this.d.populateQuery();
		return this.d.selectableQuery(qa);
	}
	
	
	// String più tipo: 0 - regola, 1 - fatto, 2 - atomo_to_explain, 3 - regola_con_aggregato
	public List<Response> justify(List<QueryAtom> chain, QueryAtom atom) {
		return null;
	}
	
	public List<Map<String, List<String>>> expandAggregate(String rule, List<String> answer_set) {
		return null;	
	}
	
	
}
