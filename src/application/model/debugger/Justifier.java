package application.model.debugger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;


public class Justifier {
	private String program;
	private Debugger d;
	private List<QueryAtom> qa;
	private JSONObject output;
	
	
	public Justifier(String program, Boolean debug_rules, Boolean debug_AS) {
		this.program = program;
		this.d = new Debugger(debug_rules, debug_AS, program);
	} 
	
	public QueryAtom deriveQueryAtom(String atom) {
		for (QueryAtom tmp_atom : qa) {
			if (tmp_atom.getAtom().equals(atom.replace("not ","").replace(".", ""))) {
					return tmp_atom;
					}
			}
		return new QueryAtom(atom, 0);
	}
	
	public List<QueryAtom> getAnswerSet() {
		return qa;
	}
	
	public List<String> computeAnswerSets(Integer n) throws IOException {
		Boolean is_sat = this.d.computeAnswerSets(this.program, n);
		if (is_sat) {
			this.output = this.d.getJSONArrayOutput();
			return this.d.getAnswerSets();
		} else {
			return null;
		}
	}
	
	public List<QueryAtom> retrieveAtoms(Integer i) throws IOException {
		this.d.getFacts(this.program);
		this.d.ComputeAtomsDerived(i);
		qa = this.d.populateQuery();
		return qa;
	}
	
	public List<QueryAtom> computeFirstAnswerSet() throws IOException {
		this.d.getFacts(this.program);
		Boolean is_sat = this.d.computeAtoms(this.program);
		if (is_sat) {
			qa = this.d.populateQuery();
			return qa;
		}
		else {
			return null;
		}	
	}
	
	public List<CostLevel> requestCostLevel() {
		return this.d.getCostLevel();
	}
	
	public Boolean optProblem() {
		return this.d.isOpt(); 
	}
	
	// String più tipo: 0 - regola, 1 - fatto, 2 - atom_to_explain, 3 - regola_con_aggregato
	public List<Response> justify(List<QueryAtom> chain, QueryAtom atom, Boolean checkOpt) {
		UnsatisfiableCore unsatCore = d.debug(atom, chain, qa, program, checkOpt);
		return unsatCore.getRules();
	}

	public List<Response> justifyCost(CostLevel level, Boolean checkOpt) {
		UnsatisfiableCore unsatCore = d.debug(level.getLevel(), qa, program, checkOpt);
		return unsatCore.getRules();
	}
	
	public List<Response> debug() {
		UnsatisfiableCore unsatCore = d.debug(program);
		return unsatCore.getRules();
	}
	
	public Map<String,Map<String, List<String>>> expandAggregate(String rule, List<QueryAtom> answerSet) {
		Map<String,Map<String, List<String>>> setToShow = d.generateSet(rule);
		return setToShow;
	}
	
	public String truthAggregate(String rule, String external) {
		return d.getTruthAggregate(rule, external);
	}
	
}
