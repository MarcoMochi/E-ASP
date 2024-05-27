package application.model.debugger;

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
	
	public QueryAtom deriveQueryAtom(String atom) {
		for (QueryAtom tmp_atom : qa) {
			if (tmp_atom.getAtom().equals(atom.replace("not ","").replace(".", ""))) {
					return tmp_atom;
					}
			}
		return new QueryAtom(atom, 0);
	}
	
	public List<QueryAtom> computeFirstAnswerSet() throws IOException {
		this.d.getFacts(this.program);
		this.d.computeAtoms(this.program);
		qa = this.d.populateQuery();
		return qa;
	}
	
	
	// String più tipo: 0 - regola, 1 - fatto, 2 - atom_to_explain, 3 - regola_con_aggregato
	public List<Response> justify(List<QueryAtom> chain, QueryAtom atom) {
		UnsatisfiableCore unsatCore = d.debug(atom, chain, qa, program);
		return unsatCore.getRules();
	}
	
	public Map<String,Map<String, List<String>>> expandAggregate(String rule, List<QueryAtom> answerSet) {
		
		Map<String,Map<String, List<String>>> setToShow = d.generateSet(rule);
		return setToShow;
	}
	
	
}
