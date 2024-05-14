package debugger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.util.Pair;

public class Debugger {

	private Process process;
	private HashMap<String, Integer> myMap;
	private HashSet<String> oldQueries;
	private File f;
	private List<String> debugAtoms;
	private List<String> initialFacts;
	private List<String> derivedAtoms;
	private List<String> falseAtoms;
	private List<String> rules;
	private List<String> order;
	private String analyzed;
	boolean onlyFacts;
	boolean justify;
	private List<String> rulesIgnored;
	private List<String> order_analyzed;
	private List<UnsatisfiableCore> stackCore;
	private Boolean debug_rules;
	private Boolean debug_AS;
	private List<String> unsupported;

	public Debugger(File f, boolean onlyFacts) {
		myMap = new HashMap<String, Integer>();
		oldQueries = new HashSet<String>();
		this.f = f;
		this.onlyFacts = onlyFacts;
		if(!onlyFacts) {
			try {
				computeAtoms();
			} catch (IOException e) {
				return;
			}
		}
	}
	
	public Debugger(File f, boolean onlyFacts, boolean justify, boolean deb_rules, boolean deb_AS) throws IOException {
		myMap = new HashMap<String, Integer>();
		oldQueries = new HashSet<String>();
		this.rulesIgnored = new ArrayList<String>();
		this.order_analyzed = new ArrayList<String>();
		this.stackCore = new ArrayList<UnsatisfiableCore>();
		this.f = f;
		this.onlyFacts = onlyFacts;
		this.justify = true;
		this.debug_rules = deb_rules;
		this.debug_AS = deb_AS;
		this.unsupported = new ArrayList<String>();
		
		if(!onlyFacts & justify) {
			try {
				getFacts(f);
				computeAtoms(justify, true);
			} 
			catch (IOException e) {
				throw e;
			}
		} 
	}

	public void stopDebug() {
		if (process != null)
			process.destroy();
	}
	
	public File returnFile() {
		return f;
	}

	public boolean canBeQueried() {
		if(onlyFacts)
			return false;
		return oldQueries.size() < myMap.keySet().size();
	}

	void computeAtoms() throws IOException {
		String output = launchSolver(f, "--output=smodels", "--configuration=auto");
		String[] lines = output.split("\n");

		boolean start = false;
		String line;
		for (int i = 0; i < lines.length; i++) {
			line = lines[i];
			if (line.startsWith("0")) {
				start = !start;
				if (!start)
					break;
			} else {
				if (start) {
					String atom = line.split(" ")[1];
					if (!myMap.containsKey(atom)) {
						myMap.put(atom, 0);
					}
				}
			}
		}
	}
	
	void computeAtoms(boolean justify, boolean get_order) throws IOException {
		derivedAtoms = new ArrayList<String>();
		falseAtoms = new ArrayList<String>();
		File helper = new File(DebuggerUtil.helper);
		String tmp = fileToString(helper);
		tmp = tmp + fileToString(f);
		File tmpFile = stringToTmpFile(tmp);
		System.out.print(tmp);
		String output = launchSolver(tmpFile, "--models=1", "--outf=2", true);
		String output_info = fileToString(new File(".tmp_file2")); 
		String output_split = output_info.split("start:")[1];
		String[] output_tmp = output_split.split("-mid-");
		String grounded_tmp = output_tmp[0];
		String order_tmp = output_tmp[1];
		order_tmp = order_tmp.replace("\n", "");
		ArrayList<String> grounded = new ArrayList<String>(Arrays.asList(grounded_tmp.split(";")));
		order = new ArrayList<String>(Arrays.asList(order_tmp.split(";")));
		try {
			JSONObject obj = new JSONObject(output);
			JSONArray arr = (JSONArray) obj.get("Call");
			JSONArray model = (JSONArray) arr.getJSONObject(0).get("Witnesses");
			JSONArray answerset = (JSONArray) model.getJSONObject(0).get("Value");
			for (int j = 0; j < answerset.length(); j++) {
				String atom = answerset.getString(j);
				if (atom.startsWith("__debug") || atom.equals(""))
					continue;
				if (!initialFacts.contains(atom)) {
					derivedAtoms.add(atom);
					if (this.falseAtoms.contains(atom))
						this.falseAtoms.remove(atom);
					}	
				}
				for (String ground : grounded) {
					if (!derivedAtoms.contains(ground) & !initialFacts.contains(ground) & !(ground.equals("") & !this.falseAtoms.contains(ground)))
						this.falseAtoms.add(ground);
				}
			}
			
			catch (Exception e) {
				throw e;
			}
		
	}

	public UnsatisfiableCore debug() {
		return debug(new ArrayList<QueryAtom>());
	}

	public UnsatisfiableCore debug(List<QueryAtom> queries) {				
		try {
			if(onlyFacts && !queries.isEmpty())
				throw new Exception("Queries are not possible in case of facts debugging");
			String program = fileToString(f);
			String extendedProgram = extendProgram(program, queries);
			return computeMinimalCore(extendedProgram);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public UnsatisfiableCore debug(List<QueryAtom> queries, boolean justify) {				
		try {
			if(onlyFacts && !queries.isEmpty())
				throw new Exception("Queries are not possible in case of facts debugging");
			String program = fileToString(f);
			program = addDerived(program, queries);
			if (justify == false) {
				program = addInformation(program);
			}
			program = setRulesForOrder(queries,program);
			String extendedProgram = extendProgram(program, queries);
			return computeMinimalCore(extendedProgram);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private String setRulesForOrder(List<QueryAtom> queries, String program) {
		this.analyzed = null;
		for(QueryAtom q : queries) {
			if (q.getValue() == QueryAtom.FALSE) {
				this.analyzed = q.getAtom();
				break;
			}
		}
		if (this.analyzed == null) 
			return program;
		
		boolean ignore = false;
		List<String> atoms = new ArrayList<String>();
		List<String> order_loc = getOrder();
		if (order_loc.contains(this.analyzed)) {
			for(String i : order_loc) {
				if (i.equals(this.analyzed)) {
					ignore = true;
					continue;
				}
				if (ignore)
					atoms.add(i);
			}
		}
		
		
		StringBuilder builder = new StringBuilder();
		for(String line : program.split("\n")) {
			boolean temp = false;
			for (String atom : atoms) {
				if (line.contains(atom)) {
					builder.append(line + "@ignore" + "\n");
					temp = true;
					break;
				}
			}
			if (!temp) {
					builder.append(line + "\n");
				}
		}
		return builder.toString();
	}
	
	private String addInformation(String program) {
		StringBuilder builder = new StringBuilder();
		for(String line : program.split("\n")) {
			if (rulesIgnored.contains(line))
					builder.append(line + "@ignore" + "\n");
			else
				builder.append(line + "\n");
		}
		return builder.toString();
	}

	private String addDerived(String program, List<QueryAtom> queries) {
		StringBuilder builder = new StringBuilder();
		builder.append("%Add Answer Set\n");
		for (QueryAtom q : queries) {
			if (q.getValue() == QueryAtom.FALSE) {
				if (falseAtoms.contains(q.getAtom()))
					builder.append(":- not " + q.getAtom() + ".\n");
				else
					builder.append(":- " + q.getAtom() + ".\n");
			}
			else if (q.getValue() == QueryAtom.TRUE) {
				if (falseAtoms.contains(q.getAtom()))
					builder.append(":- " + q.getAtom() + ".\n");
				else
					builder.append(":- not " + q.getAtom() + ".\n");
			}
			//else if (derivedAtoms.contains(q.getAtom()) & this.order.contains(q.getAtom()))
			else if (derivedAtoms.contains(q.getAtom())) {
				builder.append(":- not " +q.getAtom() + ".");
				if (this.order_analyzed.contains(q.getAtom()) & !(q.getAtom().equals(order_analyzed.get(order_analyzed.size()-1))))
					builder.append("@ignore\n");
				else
					builder.append("\n");
			}
			else if (falseAtoms.contains(q.getAtom())) {
				builder.append(":- " +q.getAtom() + ".");
				if (this.order_analyzed.contains(q.getAtom()) & !(q.getAtom().equals(order_analyzed.get(order_analyzed.size()-1))))
					builder.append("@ignore\n");
				else
					builder.append("\n");
			}
		}
		return program + builder.toString();
	}

	public List<QueryAtom> computeQuery(UnsatisfiableCore unsatCore) throws IOException {
		int numModels = 0;
		ArrayList<QueryAtom> qa = new ArrayList<QueryAtom>();

		if (unsatCore.getLines().size() <= 1)
			return qa;

		for (int i = 0; i < unsatCore.getRules().size(); i++) {
			StringBuilder enc = new StringBuilder();
			for (int j = 0; j < unsatCore.getRules().size(); j++) {
				if (i != j) {
					String rule = unsatCore.getRules().get(j);
					enc.append(rule+"\n");
				}
			}
			String res = extendProgram(enc.toString(), new ArrayList<QueryAtom>());
			numModels += query(res);
		}
		int mean = numModels / 2;
		ArrayList<Pair<String, Integer>> myList = new ArrayList<Pair<String, Integer>>();
		for (String i : myMap.keySet()) {
			myList.add(new Pair<String, Integer>(i, Math.abs(myMap.get(i) - mean)));
		}
		myList.sort(new Comparator<Pair<String, Integer>>() {

			@Override
			public int compare(Pair<String, Integer> o1, Pair<String, Integer> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		int i = 0;
		int count = 0;
		while (i < myList.size() && count < 5) {
			if (oldQueries.add(myList.get(i).getKey())) {
				qa.add(new QueryAtom(myList.get(i).getKey(), QueryAtom.NOT_SET));
				count++;
			}
			i++;
		}
		return qa;
	}
	
	public List<QueryAtom> populateQuery() {
		ArrayList<QueryAtom> qa = new ArrayList<QueryAtom>();
		for (String i : initialFacts) {
			qa.add(new QueryAtom(i, QueryAtom.NOT_SET));
		}
		for (String i : derivedAtoms) {
			qa.add(new QueryAtom(i, QueryAtom.NOT_SET));
		}
		for (String i : falseAtoms) {
			qa.add(new QueryAtom(i, QueryAtom.NOT_SET));
		}
		for (String i : rules) {
			qa.add(new QueryAtom(i, QueryAtom.NOT_SET));
		}
		
		
		return qa;
	}
	
	public List<String> getDerivedAtoms() {
		return derivedAtoms;
	}
	
	public List<String> getFalseAtoms() {
		return falseAtoms;
	}

	private File stringToTmpFile(String program) {
		File f = new File(".tmp_file");
		try {
			FileWriter fw = new FileWriter(f, false);
			fw.append(program);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return f;
	}


	private String fileToString(File f) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		StringBuilder builder = new StringBuilder();
		while (br.ready()) {
			String line = br.readLine();
			if (!line.contains("#show"))
				//if (!line.contains(":-") && line.contains("..")) {
				//	for (String temp_atom : transform_line(line)) {
				//		builder.append(temp_atom + "\n");
				//	}
				//continue;
				//}
				builder.append(line + "\n");
		}
		br.close();
		return builder.toString();
	}
	
	private void getFacts(File f) throws IOException {
		String program = fileToString(f);
		StringBuilder tempProgram = new StringBuilder();
		initialFacts = new ArrayList<String>();
		rules = new ArrayList<String>();
		String output = launchSolver(stringToTmpFile(program.toString()),  "--mode=gringo", "--text");
		for(String line : program.split("\n")) {
			if (!line.contains(":-") && !line.contains(":~") && !line.contains("{") && line.length() > 0) {
				tempProgram.append(line);
			} else if (line.contains(":-"))  {
				rules.add(line.substring(0, line.trim().length()-1));
			}
		}
		if (tempProgram.toString().length() > 0) {
			output = launchSolver(stringToTmpFile(tempProgram.toString()),  "--mode=gringo", "--text");
			for (String atom : output.split("\n")) {
				if (atom.length() > 0)
					initialFacts.add(atom.substring(0, atom.length()-1));
			}
		}
	}	


	private String extendProgram(String p, List<QueryAtom> queries) {
		debugAtoms = new ArrayList<String>();
		String[] lines = p.split("\n");
		StringBuilder builder = new StringBuilder();
		int cont = 0;
		String info = "% no description";
		Boolean start_AS = false;
		for (int i = 0; i < lines.length; i++) {
			cont++;
			String line = lines[i].trim();
			if (line.startsWith("%@description: ")) {
				info = line.replaceAll("@description:", "");
				continue;
			}
			if (line.equals("%Add Answer Set"))
				start_AS = true;
			if (line.startsWith("%"))
				continue;
			if (line.isEmpty())
				continue;
			if (line.contains("#const")) {
				builder.append(line + "\n");
				continue;
			}
			if (line.contains("@correct")) {
				line = line.replace("@correct", "");
				continue;
			}
			if (line.contains("@ignore")) {
				line = line.replace("@ignore", "");
				continue;
			}
			
			String line_parsed = line.replace("\"", "\\\"");
			if(onlyFacts) {		
				if (!line.contains("."))
					continue;
				
				if (line.contains(".") && !line.contains(":-")) {
					String deb = "__debug(\"" + line_parsed + "\",\"" + info + "\"," + cont + ")";
					debugAtoms.add(deb);
					line = line.substring(0, line.length() - 1) + ":- not " + deb + ".";
					builder.append(line + "\n");
					builder.append("{" + deb + "}.\n");
				}	
				else {
					builder.append(line + "\n");
				}
			}
			else {
				
				if (!start_AS) {
					if(debug_rules) {
				
					String deb = "__debug(\"" + line_parsed + "\",\" Supporting rule \"," + cont + ")";
					debugAtoms.add(deb);
		
					if (line.contains(":-")) {
						line = line.substring(0, line.length() - 1) + ", not " + deb + ".";
					} else if (line.contains(".")) {
						line = line.substring(0, line.length() - 1) + ":- not " + deb + ".";
					} else {
						continue;
					}
					builder.append(line + "\n");
					builder.append("{" + deb + "}.\n");
				} else {
					builder.append(line + "\n");
					}
				}
				
				if (start_AS) {
					
					String sup = "__support(\"" + line_parsed + "\",\"" + info + "\"," + cont + ")";
					debugAtoms.add(sup);
					
					String atom_tmp = null;
					if (line.contains(":- not")) 
						atom_tmp = line.replace(":- not ", "");
					else if (line.contains(":- ")) 
						atom_tmp = line.replace(":- ", "");
					else
						continue;
					
					
					builder.append(atom_tmp.substring(0, atom_tmp.length()-1) + " :- " + sup + ".\n");
					builder.append("{" + sup + "}.\n");
					
					if(debug_AS) {
				
						String deb = "__debug(\"" + line_parsed + "\",\" Supporting derived atom \"," + cont + ")";
						debugAtoms.add(deb);
				
						if (line.contains(":-")) {
							line = line.substring(0, line.length() - 1) + ", not " + deb + ".";
						} else if (line.contains(".")) {
							line = line.substring(0, line.length() - 1) + ":- not " + deb + ".";
						} else {
							continue;
						}
						builder.append(line + "\n");
						builder.append("{" + deb + "}.\n");
				} else {
					builder.append(line + "\n");
					}
				}
					
				

			}
			if (!info.equalsIgnoreCase("% no description"))
				info = "% no description";
		}
		
		for (QueryAtom q : queries) {
			if (q.getValue() == QueryAtom.TRUE)
				if (falseAtoms.contains(q.getAtom()))
					builder.append(":- " + q.getAtom() + ".\n");
				else
					builder.append(":- not " + q.getAtom() + ".\n");
			else if (q.getValue() == QueryAtom.FALSE)
				if (falseAtoms.contains(q.getAtom()))
					builder.append(":- not " + q.getAtom() + ".\n");
				else
					builder.append(":- " + q.getAtom() + ".\n");
			else
				continue;

		}

		return builder.toString();
	}
	
	private boolean checkCoherence(String extendedProgram, List<String> core) throws IOException {
		String tmp = extendedProgram;
		for (int i = 0; i < core.size(); i++) {
			if (!core.get(i).contains("show"))
				tmp += ":- " + core.get(i) + ".\n";
		}		
		return (!isIncoherent(stringToTmpFile(tmp), "--outf=1", "--keep-facts"));			
	}

	private UnsatisfiableCore computeMinimalCore(String extendedProgram) throws Exception {
		UnsatisfiableCore unsatCore = new UnsatisfiableCore();	
		if (this.unsupported.contains(this.analyzed)) {
			unsatCore.addRule("Atom has no rules to support it", "Unsupported", 0);
			return unsatCore;
		}
		
		// Check Coherence is True when the program is sat
		if(checkCoherence(extendedProgram, debugAtoms))
			return unsatCore;
		
		
		//The program is known to be incoherent under all assumptions!
		List<String> smallCore = new ArrayList<String>();
		int value = 1;
		while(checkCoherence(extendedProgram, smallCore)) {
			smallCore = new ArrayList<String>();
			for(int i = 0; i < value && i < debugAtoms.size(); i++)
				smallCore.add(debugAtoms.get(i));
			value *= 2;
		}
		List<String> core = smallCore;
		
		List<String> minimalCore = new ArrayList<String>();
		while (!core.isEmpty()) {
			String tmp = extendedProgram;
			String last = core.remove(core.size() - 1);
			for (int i = 0; i < core.size(); i++) {
				tmp += ":- " + core.get(i) + ".\n";
			}
			for (int i = 0; i < minimalCore.size(); i++) {
				tmp += ":- " + minimalCore.get(i) + ".\n";
			}
			if (!isIncoherent(stringToTmpFile(tmp), "--outf=1", "--keep-facts"))
				minimalCore.add(last);
		}
		
		for (String s : minimalCore) {
			Pattern pattern = Pattern.compile("__debug\\((\".*\"),(\".*\"),(.*)\\)");
			Matcher m = pattern.matcher(s);
			if (m.find()) {
				unsatCore.addRule(m.group(1).replaceAll("\"", "").replace("\\", ""), m.group(2).replaceAll("\"", ""),
						Integer.parseInt(m.group(3)));
			}
		}
		
		for (String s : minimalCore) {
			Pattern pattern = Pattern.compile("__support\\((\".*\"),(\".*\"),(.*)\\)");
			Matcher m = pattern.matcher(s);
			if (m.find()) {
				
				System.out.print("Got supported: " + m.group(1).replaceAll("\"", ""));
				
				if (this.debug_rules) {
					List<String> head_rules = searchHead(m.group(1).replaceAll("\"", ""), extendedProgram);
					for (String head : head_rules) {
						unsatCore.addRule(head, "% no description".replaceAll("\"", ""),
								0);
					}
				}
				
				if (this.debug_AS) {
					if (this.unsupported.contains(m.group(1).replaceAll("\"", "").replace(":-", "").replace(".", "").trim())) {
						unsatCore.addRule(m.group(1).replaceAll("\"", "").replace("\\", ""), "% no description".replaceAll("\"", ""),
								0);
						continue;
					}
					
					if (!m.group(1).replaceAll("\"", "").replace(":-", "").replace(" not ", "").replace(".", "").trim().equals(this.analyzed)) {
						unsatCore.addRule(m.group(1).replaceAll("\"", "").replace("\\", ""), "% no description".replaceAll("\"", ""), 0);
							continue;
						}
					}
				
				}
			}
		
		System.out.print("Unsat Core" +unsatCore);
		
		
		return unsatCore;
	}
	
	private List<String> searchHead(String atom, String program) {
		ArrayList<String> rules = new ArrayList<String>(); 
		String head = atom.split(":-")[1].split(Pattern.quote("("))[0].replace("not", "").replace(".", "").trim();
		int arity;
		if (atom.split(":-")[0].contains("("))
			arity = getArity(atom.split(":-")[0].split(Pattern.quote("("))[1]);
		else
			arity = getArity(atom.split(":-")[0]);
		
		for(String line : program.split("\n")) {
			if (!line.startsWith("{__debug") && !line.contains("__support") && line.contains(":-") && line.split(":-")[0].length() > 0) {
				String tmp_head = line.split(":-")[0].split(Pattern.quote("("))[0].trim();
				int tmp_arity;
				if (tmp_head.split(":-")[0].contains("("))
					tmp_arity = getArity(line.split(":-")[0].split(Pattern.quote("("))[1]);
				else
					tmp_arity = getArity(line.split(":-")[0]);
				
				if (head.equals(tmp_head) && arity == tmp_arity) {
					Pattern pattern = Pattern.compile("__debug\\((\".*\"),(\".*\"),(.*)\\)");
					Matcher m = pattern.matcher(line);
					if (m.find()) {
						rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
					}
				}
			}
		}
		return rules;
	}
		
	private int getArity(String params) {
		int arity = 0;
		boolean skip = false;
		for(int i = 0; i < params.length(); i++) {
			if (params.charAt(i) == '"' || params.charAt(i) == '\'') 
				skip = !skip;
			if (!skip) {
				if (params.charAt(i) == ',') {
					arity += 1;
				}
			}
		}
	return arity;
	}
	
	
	private boolean isIncoherent(File encoding, String option1, String option2) throws IOException {
		process = new ProcessBuilder(DebuggerUtil.solver, encoding.getAbsolutePath(), option1, option2).start();
		while (process.isAlive()) {
		}

		int exit_code = process.exitValue();
		process = null;
		return exit_code == 20;
	}
	
	public void setIgnore(String rule) {
		this.rulesIgnored.add(rule);
	}
	
	public String backAnalyzed() {
		return order_analyzed.remove(order_analyzed.size()-1);
	}
	
	public String getAnalyzed() {
		if (order_analyzed.size() > 0)
			return this.order_analyzed.get(order_analyzed.size()-1);
		return null;
	}
	
	public void addAnalyzed(String atom) {
		this.order_analyzed.add(atom);
	}
	
	public void addCore(UnsatisfiableCore core) {
		this.stackCore.add(core);
	}
	
	public void backCore() {
		if (stackCore.size() > 0)
			this.stackCore.remove(stackCore.size() - 1);
	}
	
	
	public UnsatisfiableCore getCore() {
		if (stackCore.size() > 0)
			return this.stackCore.get(stackCore.size() - 1);
		return null;
	}
	
	
	public List<String> getOrder() {
		return this.order;
	}
	
	public void updateQuery(List<QueryAtom> queries) {
		
		String new_analyzed = this.getAnalyzed();
		if (new_analyzed == null)
			return;
		
		for(QueryAtom q : queries) {
			if (!q.getAtom().equals(new_analyzed))
				q.setValue(QueryAtom.NOT_SET);
			else
				q.setValue(QueryAtom.FALSE);
		}
	}
	
	public List<Object> generateSet(List<String> rules, String aggregate) {
		HashMap<String,HashMap<String, List<String>>> totalSet = new HashMap<String, HashMap<String, List<String>>>();
		HashMap<String,HashMap<String, List<String>>> optSet = new HashMap<String, HashMap<String, List<String>>>();
		
		StringBuilder builder = new StringBuilder();
		builder.append(aggregate + "\n");
		for (String rule : this.initialFacts)
			builder.append("#external " + rule + ".\n");
		for (String rule : this.derivedAtoms)
			builder.append("#external " + rule + ".\n");
		for (String rule : this.falseAtoms)
			builder.append("#external " + rule + ".\n");
		
		
		String output = launchSolver(stringToTmpFile(builder.toString()),  "--mode=gringo", "--text");
		String[] grounded = output.split("\n");
		Pattern pattern = Pattern.compile("\\{(.*?)\\}");
		String to_delete = "";
		String message = "";
		HashMap<String, String> created = new HashMap<String, String>();
		for (String atom : grounded) {
			if (atom.startsWith("#external"))
				continue;
			if (atom.startsWith("#")) {
				if (atom.contains(":-") && atom.split(":-")[0].length() > 0  && atom.split(":-")[1].length() > 0) {
					created.put(atom.split(":-")[0].trim(), atom.split(":-")[1].trim());
				continue;
				}
			}
				
			Matcher matcher = pattern.matcher(atom);
			if (matcher.find()) {
				String tmp_outside = "";
				if (atom.contains(":-")) 
					tmp_outside = atom.split(":-")[1];
				else if (atom.contains("<=>"))
					tmp_outside = atom.split("<=>")[1];
				else
					continue;
				StringBuilder outside = new StringBuilder();
				boolean copy = true; 
				for (Entry<String, String> gr_text : created.entrySet()) {
					if (tmp_outside.contains(gr_text.getKey()))
						tmp_outside = tmp_outside.replace(gr_text.getKey(), gr_text.getValue().substring(0, gr_text.getValue().length() - 1));
				}
				for(int i = 0; i < tmp_outside.length()-1; i++) {
					if (tmp_outside.substring(i, i+1).equals("#"))
						copy = false;
					if (tmp_outside.substring(i, i+1).equals("}")) {
						copy = true;
						continue;
					}
					if (copy)
						outside.append(tmp_outside.substring(i, i+1));
				}
				
				for(String block : outside.toString().split(",")) {
					if (block.contains("=") || block.contains("<") || block.contains(">")) {
						to_delete = block;
					}
				}
				
				String temp = matcher.group().substring(1, matcher.group().length()-1);
				boolean internal = getIdSet(temp, outside.toString().replace(to_delete, ""), totalSet);
				
				
				if (aggregate.contains("#count")) {
					if (to_delete.contains(">") || to_delete.contains("<")) {
						HashMap<String, List<String>> entry = totalSet.get(outside.toString().replace(to_delete, ""));
						message = inspectCount(optSet, outside.toString().replace(to_delete, ""), entry, to_delete);
						}
				}
				else if (aggregate.contains("#sum")) {
					if (to_delete.contains(">") || to_delete.contains("<")) {
						HashMap<String, List<String>> entry = totalSet.get(outside.toString().replace(to_delete, ""));
						message = inspectCount(optSet, outside.toString().replace(to_delete, ""), entry, to_delete);
						}
				}
				
				
			}
		}
		
		setFalseTrue(totalSet);
		List<Object> final_values = new ArrayList<Object>();
		List<HashMap<String,HashMap<String, List<String>>>> return_values = new ArrayList<HashMap<String,HashMap<String, List<String>>>>();
		return_values.add(totalSet);
		if (to_delete.contains(">") || to_delete.contains("<")) 
			return_values.add(optSet);
		else {
			return_values.add(totalSet);
			message = "Showing all the positive and negative atoms";
		}
		final_values.add(return_values);
		final_values.add(message);

		return final_values;
	}
	
	private boolean getIdSet(String body, String new_key, HashMap<String,HashMap<String, List<String>>> totalSet) {
		HashMap<String, List<String>> set = new HashMap<String, List<String>>();
		String[] sets = body.split(";");
		boolean found = false;
		for (String block : sets) {
			String temp_ID = block.split(":")[0];
			String temp_body = block.split(":")[1];
			if (!found & temp_body.contains(this.analyzed))
				found = true;
			if (!set.containsKey(temp_ID)) {
	            set.put(temp_ID, new ArrayList<String>());
	        }
	        set.get(temp_ID).add(temp_body);
		}
		totalSet.put(new_key, set);
		return found;
	}
	
	private void setFalseTrue(HashMap<String,HashMap<String, List<String>>> set) {
		
		for (Entry<String, HashMap<String, List<String>>> map : set.entrySet()) {
			String key_global = map.getKey();
			for (Entry<String, List<String>> mapping : map.getValue().entrySet()) {
				String key_local = mapping.getKey();
				int index = 0;
				for (String atom : mapping.getValue()) {
					if (atom.contains("not ") && (this.falseAtoms.contains(atom.replace("not ", ""))))
						set.get(key_global).get(key_local).get(index).replace("not ", "");
					else if (!atom.contains("not ") && !(this.derivedAtoms.contains(atom) || this.initialFacts.contains(atom)))
						set.get(key_global).get(key_local).set(index, "not " + set.get(key_global).get(key_local).get(index));
					index++;
				}
			}
		}
	}
	
	private HashMap<String, List<String>> findTrueAggUntil(HashMap<String, List<String>> entry, int value_guard, int slack, boolean less) {
		List<Integer> avoid = new ArrayList<Integer>();
		HashMap<String, List<String>> set = new HashMap<String, List<String>>();
		int counter = 0;
		Integer order_set = 0;
		ArrayList<ArrayList<String>> last_set = new ArrayList<ArrayList<String>>();
		for (Entry<String, List<String>> key_values : entry.entrySet()) {
			for (String to_add : key_values.getValue()) {
				if (!(this.order.contains(to_add) || this.order.contains(to_add.replace("not ", "")) || 
						this.order.contains("not ".concat(to_add)))) {
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(key_values.getKey());
					temp.add(to_add);
					last_set.add(temp);
				}
			}
		}
		
		for(String check_atom : this.order) {
			int tmp_index = 0;
			if ((less && counter <= value_guard) || (!less && counter < (value_guard + slack))) {
				for (List<String> values : entry.values()) {
					tmp_index ++;
					if (avoid.contains(tmp_index))
						break;
					if (values.contains(check_atom)) {
							avoid.add(tmp_index);
							List<String> temp = new ArrayList<String>();
							temp.add(check_atom);
							set.put(order_set.toString(), temp);
							order_set++;
							counter++;
							break;
						}
					}
			} else {
				return set;
				}
			}
		
		for (ArrayList<String> key_atom : last_set) {
			String check_atom = key_atom.get(1);
			if ((less && counter <= value_guard) || (!less && counter < (value_guard + slack))) {
				if (avoid.contains(Integer.parseInt(key_atom.get(0))))
						break;
				if ((check_atom.contains("not ") && this.falseAtoms.contains(check_atom.replace("not ", ""))) ||
							(!check_atom.contains("not ") && (this.derivedAtoms.contains(check_atom) || this.initialFacts.contains(check_atom)))) {
						 
						avoid.add(Integer.parseInt(key_atom.get(0)));
						List<String> temp = new ArrayList<String>();
						temp.add(check_atom);
						set.put(order_set.toString(), temp);
						order_set++;
						counter++;
						break;
					}
				} else 
					return set;
			}
		return set;
	}
	
	
	private HashMap<String, List<String>> findTrueAggUntilSum(HashMap<String, List<String>> entry, int value_guard, int slack, boolean less) {
		List<String> avoid = new ArrayList<String>();
		HashMap<String, List<String>> set = new HashMap<String, List<String>>();
		int counter = 0;
		
		ArrayList<ArrayList<String>> last_set = new ArrayList<ArrayList<String>>();
		for (Entry<String, List<String>> key_values : entry.entrySet()) {
			for (String to_add : key_values.getValue()) {
				if (!(this.order.contains(to_add) || this.order.contains(to_add.replace("not ", "")) || 
						this.order.contains("not ".concat(to_add)))) {
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(key_values.getKey());
					temp.add(to_add);
					last_set.add(temp);
				}
			}
		}
		
		for(String check_atom : this.order) {
			int tmp_index = 0;
			if ((less && counter <= value_guard) || (!less && counter < (value_guard + slack))) {
				for (Entry<String, List<String>> mapping : entry.entrySet()) {
					tmp_index ++;
					if (avoid.contains(mapping.getKey()))
						break;
					if (mapping.getValue().contains(check_atom)) {
						avoid.add(mapping.getKey());
						List<String> temp = new ArrayList<String>();
						temp.add(check_atom);
						set.put(mapping.getKey(), temp);
						counter += Integer.parseInt(mapping.getKey().split(",")[0]);
						break;
					}
				}
			} else {
				return set;
				}
			}
		
		for (ArrayList<String> key_atom : last_set) {
			String check_atom = key_atom.get(1);
			if ((less && counter <= value_guard) || (!less && counter < (value_guard + slack))) {
				if (avoid.contains(key_atom.get(0)))
						break;
				if ((check_atom.contains("not ") && this.falseAtoms.contains(check_atom.replace("not ", ""))) ||
							(!check_atom.contains("not ") && (this.derivedAtoms.contains(check_atom) || this.initialFacts.contains(check_atom)))) {
						 
						avoid.add(key_atom.get(0));
						List<String> temp = new ArrayList<String>();
						temp.add(check_atom);
						set.put(key_atom.get(0), temp);
						counter += Integer.parseInt(key_atom.get(0).split(",")[0]);
						break;
					}
				} else 
					return set;
			}
		return set;
	}
	
	private HashMap<String, List<String>> findFalseAggUntil(HashMap<String, List<String>> entry, int value_guard, int slack, boolean less) {
		HashMap<String, List<String>> set = new HashMap<String, List<String>>();
		int counter = 0;
		int total_atoms = 0;
		ArrayList<ArrayList<String>> last_set = new ArrayList<ArrayList<String>>();
		for (Entry<String, List<String>> key_values : entry.entrySet()) {
			total_atoms += key_values.getValue().size();
			for (String to_add : key_values.getValue()) {
				if (!(this.order.contains(to_add) || this.order.contains(to_add.replace("not ", "")) || 
						this.order.contains("not ".concat(to_add)))) {
					ArrayList<String> temp = new ArrayList<String>();
					temp.add(key_values.getKey());
					temp.add(to_add);
					last_set.add(temp);
				}
			}
		}
		
		for(String check_atom : this.order) {
			if ((!less && counter < (total_atoms - (value_guard - slack)) ) || (less && counter <= (total_atoms - value_guard))) {
				for (Entry<String, List<String>> key_values : entry.entrySet()) {
					List<String> values = key_values.getValue();
					String temp_atom = check_atom;
					if (check_atom.contains("not ")) 
						temp_atom = temp_atom.replace("not ", "");
					else
						temp_atom = "not " + temp_atom;
					if (values.contains(temp_atom)) {
						if (!set.containsKey(key_values.getKey())) {
							List<String> temp = new ArrayList<String>();
							temp.add(check_atom);
							set.put(key_values.getKey(), temp);
							counter++;
						} else {
							set.get(key_values.getKey()).add(check_atom);
							counter++;
						}
					}
				}
			} else {
				return set;
			    }
		}
		for (ArrayList<String> key_atom : last_set) {
			String check_atom = key_atom.get(1);
			if ((!less && counter < (total_atoms - (value_guard - slack)) ) || (less && counter <= (total_atoms - value_guard))) {
				if ((check_atom.contains("not ") && (this.derivedAtoms.contains(check_atom.replace("not ", "")) || this.initialFacts.contains(check_atom.replace("not ", "")))) || 
						(!check_atom.contains("not ") && (this.falseAtoms.contains(check_atom)))) {
					if (!set.containsKey(key_atom.get(0))) {
						List<String> temp = new ArrayList<String>();
						temp.add(check_atom);
						set.put(key_atom.get(0), temp);
						counter++;
					} else {
						set.get(key_atom.get(0)).add(check_atom);
						counter++;
					}
				}
			} else {
				return set;
			}
		}
		return set;
	}
	
	private HashMap<String, List<String>> findFalseAggUntilSum(HashMap<String, List<String>> entry, int value_guard, int slack, boolean less) {
		HashMap<String, List<String>> set = new HashMap<String, List<String>>();
		int counter = 0;
		int total_atoms = 0;
		for (List<String> values : entry.values())
			total_atoms += values.size();
		for(String check_atom : this.order) {
			if ((!less && counter < (total_atoms - (value_guard - slack) )) || (less && counter <= (total_atoms - value_guard))) {
				for (Entry<String, List<String>> mapping : entry.entrySet()) {
					String temp_atom = check_atom;
					if (check_atom.contains("not ")) 
						temp_atom = temp_atom.replace("not ", "");
					else
						temp_atom = "not " + temp_atom;
					if (mapping.getValue().contains(temp_atom)) {
						if (!set.containsKey(mapping.getKey())) {
							List<String> temp = new ArrayList<String>();
							temp.add(check_atom);
							set.put(mapping.getKey(), temp);
						} else {
							set.get(mapping.getKey()).add(check_atom);
						}
						counter += Integer.parseInt(mapping.getKey());
						break;
					}
				}
			} else {
				return set;
			    }
		}
		return set;
	}
	
	private HashMap<String, List<String>> findTrueAggAll(HashMap<String, List<String>> entry, boolean sum_agg) {
		HashMap<String, List<String>> set = new HashMap<String, List<String>>();
		Integer order_set = 0;
		for (Entry<String, List<String>> map : entry.entrySet()) {
			String val = map.getKey();
			List<String> temp = new ArrayList<String>();
		    for (String tmp_atom : map.getValue()) {
		    	if ((tmp_atom.contains("not ") && this.falseAtoms.contains(tmp_atom.replace("not ", ""))) ||
		    			(!tmp_atom.contains("not ") && (this.derivedAtoms.contains(tmp_atom) ||this.initialFacts.contains(tmp_atom)))) {
		    		temp.add(tmp_atom);
					if (sum_agg) {
						set.put(val.toString(), temp);
					} else {
			    		set.put(order_set.toString(), temp);
			    		order_set++;
					}
		    	}
		    }
		}
		return set;
	}
	
	private HashMap<String, List<String>> findFalseAggAll(HashMap<String, List<String>> entry, boolean sum_agg) {
		HashMap<String, List<String>> set = new HashMap<String, List<String>>();
		Integer order_set = 0;
		for (Entry<String, List<String>> map : entry.entrySet()) {
			String val = map.getKey();
			List<String> temp = new ArrayList<String>();
		    for (String tmp_atom : map.getValue()) {
		    	if ((tmp_atom.contains("not ") && !this.falseAtoms.contains(tmp_atom.replace("not ", ""))) ||
		    			(!tmp_atom.contains("not ") && !(this.derivedAtoms.contains(tmp_atom) ||this.initialFacts.contains(tmp_atom)))) {
		    		temp.add(tmp_atom);
		    		if (sum_agg) {
						set.put(val.toString(), temp);
					} else {
			    		set.put(order_set.toString(), temp);
			    		order_set++;
					}
		    	}
		    }
		}
		return set;
	}
	
	private HashMap<String, List<String>> findAggAll(HashMap<String, List<String>> entry) {
		HashMap<String, List<String>> set = new HashMap<String, List<String>>();
		Integer order_set = 0;
		for (List<String> values : entry.values()) {
		    for (String tmp_atom : values) {
		    	List<String> temp = new ArrayList<String>();
				temp.add(tmp_atom);
		    	set.put(order_set.toString(), temp);
		    	order_set++;
		    }
		}
		return set;
	}
	
	private String inspectCount(HashMap<String,HashMap<String, List<String>>> optSet, String key, HashMap<String, List<String>> entry, String guard) {
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(guard);
		Integer value_guard = 0;
		if (m.find()) {
		  value_guard = Integer.parseInt(m.group(0));
		}
		boolean truth = check_truth(entry, guard, true);
		if (!this.derivedAtoms.contains(this.analyzed)) {
			if (truth) {
				if (guard.contains("<=")) {
					HashMap<String, List<String>> set = findTrueAggUntil(entry, value_guard, 0, false);
					optSet.put(key, set);
					return "Showing the first " + value_guard.toString() + " true atoms "; 
				} else if (guard.contains("<")) {
					HashMap<String, List<String>> set = findTrueAggUntil(entry, value_guard, 0, true);
					optSet.put(key, set);
					return "Showing the first " + value_guard.toString() + " - 1 true atoms "; 
				} else if (guard.contains(">=")) {
					HashMap<String, List<String>> set = findFalseAggUntil(entry, value_guard, 0, false);
					optSet.put(key, set);
					return "Showing the first " + value_guard.toString() + " false atoms "; 
				} else if (guard.contains(">")) {
					HashMap<String, List<String>> set = findFalseAggUntil(entry, value_guard, 0, true);
					optSet.put(key, set);
					return "Showing the first " + value_guard.toString() + " false atoms "; 
				} 
			}
		 else {
			 if (guard.contains("<=")) {
					HashMap<String, List<String>> set = findFalseAggUntil(entry, value_guard, 1, false);
					optSet.put(key, set);
					return "Showing the first " + value_guard.toString() + " - 1 false atoms causing conflict "; 
				} else if (guard.contains("<")) {
					HashMap<String, List<String>> set = findFalseAggUntil(entry, value_guard, 0, false);
					optSet.put(key, set);
					return "Showing the first " + value_guard.toString() + " false atoms causing conflict ";  
				} else if (guard.contains(">=")) {
					HashMap<String, List<String>> set = findTrueAggUntil(entry, value_guard, -1, false);
					optSet.put(key, set);
					return "Showing the first " + value_guard.toString() + " true atoms causing conflict "; 
				} else if (guard.contains(">")) {
					HashMap<String, List<String>> set = findTrueAggUntil(entry, value_guard, 0, false);
					optSet.put(key, set);
					return "Showing all the first " + value_guard.toString() + " true atoms causing conflict ";  
				}
		 	}
		} else {
			if (truth) {
				if (guard.contains("<")) {
					HashMap<String, List<String>> set = findFalseAggAll(entry, false);
					optSet.put(key, set);
					return "Showing all the false atoms ";
				} else if (guard.contains(">")) {
					HashMap<String, List<String>> set = findTrueAggAll(entry, false);
					optSet.put(key, set);
					return "Showing all the positive atoms ";
				} 
		} else {
			if (guard.contains("<")) {
				HashMap<String, List<String>> set = findTrueAggAll(entry, false);
				optSet.put(key, set);
				return "Showing all the positive atoms "; 
			} else if (guard.contains(">")) {
				HashMap<String, List<String>> set = findFalseAggAll(entry, false);
				optSet.put(key, set);
				return "Showing all the false atoms ";
				}
			}
		}
		return "";
	}
	
	private String inspectSum(HashMap<String,HashMap<String, List<String>>> optSet, String key, HashMap<String, List<String>> entry, String guard) {
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(guard);
		Integer value_guard = 0;
		if (m.find()) {
		  value_guard = Integer.parseInt(m.group(0));
		}
		int counter = 0;
		boolean truth = check_truth(entry, guard, true);
		Integer order = 0; 
		if (!this.derivedAtoms.contains(this.analyzed)) {
			if (truth) {
				if (guard.contains("<=")) {
					HashMap<String, List<String>> set = findTrueAggUntilSum(entry, value_guard, 0, false);
					optSet.put(key, set);
					return "Showing the first true atoms ";  
				} else if (guard.contains("<")) {
					HashMap<String, List<String>> set = findTrueAggUntilSum(entry, value_guard, 0, true);
					optSet.put(key, set);
					return "Showing the first true atoms ";  
				} else if (guard.contains(">=")) {
					HashMap<String, List<String>> set = findFalseAggUntilSum(entry, value_guard, 0, false);
					optSet.put(key, set);
					return "Showing the first false atoms satisfying the aggregate "; 
				} else if (guard.contains(">")) {
					HashMap<String, List<String>> set = findFalseAggUntilSum(entry, value_guard, 0, true);
					optSet.put(key, set);
					return "Showing the first false atoms satisfying the aggregate "; 
				} 
			}
		 else {
			 if (guard.contains("<=")) {
					HashMap<String, List<String>> set = findFalseAggUntilSum(entry, value_guard, -1, false);
					optSet.put(key, set);
					return "Showing the first false atoms causing conflict ";  
				} else if (guard.contains("<")) {
					HashMap<String, List<String>> set = findFalseAggUntilSum(entry, value_guard, 0, false);
					optSet.put(key, set);
					return "Showing the first false atoms causing conflict ";   
				} else if (guard.contains(">=")) {
					HashMap<String, List<String>> set = findTrueAggUntilSum(entry, value_guard, 1, false);
					optSet.put(key, set);
					return "Showing the first true atoms causing conflict ";   
				} else if (guard.contains(">")) {
					HashMap<String, List<String>> set = findTrueAggUntilSum(entry, value_guard, 0, false);
					optSet.put(key, set);
					return "Showing the first atoms causing conflict ";  
				}
		 	}
		} else {
			if (truth) {
				if (guard.contains("<")) {
					HashMap<String, List<String>> set = findFalseAggAll(entry, true);
					optSet.put(key, set);
					return "Showing all the false atoms ";
				} else if (guard.contains(">")) {
					HashMap<String, List<String>> set = findTrueAggAll(entry, true);
					optSet.put(key, set);
					return "Showing all the true atoms ";
				} 
		} else {
			if (guard.contains("<")) {
				HashMap<String, List<String>> set = findTrueAggAll(entry, true);
				optSet.put(key, set);
				return "Showing all the true atoms ";
			} else if (guard.contains(">")) {
				HashMap<String, List<String>> set = findFalseAggAll(entry, true);
				optSet.put(key, set);
				return "Showing all the false atoms ";
				}
			}
		}
		return "";
	}
	
	private boolean check_truth(HashMap<String, List<String>> map, String guard, boolean count) {
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(guard);
		int value_guard = 0;
		if (m.find()) {
		  value_guard = Integer.parseInt(m.group(0));
		}
		int counter = 0;
		if (count) {
			for (List<String> values : map.values()) {
			    for (String tmp_atom : values) {
			    	if (tmp_atom.contains("not ")) {
			    		if (this.falseAtoms.contains(tmp_atom.replace("not ", ""))) {
			    			counter ++;
			    			break;
			    		}
			    	}
			    	else {
				    	if (this.derivedAtoms.contains(tmp_atom) || this.initialFacts.contains(tmp_atom)) {
			    			counter ++;
			    			break;
			    		}
			    	}
			    }
			}
	} else {
			
		for (Entry<String, List<String>> inside : map.entrySet()) {
				int value_sum = Integer.parseInt(inside.getKey().split(",")[0]);
				boolean tmp_found = false;
				for (String tmp_atom : inside.getValue()) {
			    	if (tmp_atom.contains(":not ")) {
			    		if (this.falseAtoms.contains(tmp_atom)) {
			    			counter += value_sum;
			    			break;
			    		}
				    } else {
				   		if (this.derivedAtoms.contains(tmp_atom)) {
				   			counter += value_sum;
				   			break;				    			
				   		}
			    	}
			    }
			}
	}
				
		if (guard.contains(">")) {
			if (guard.contains("="))
				return counter <= value_guard;
			return counter < value_guard;
			}
		if (guard.contains("<")) {
			if (guard.contains("="))
				return counter >= value_guard;
			return counter > value_guard;
		}
		if (guard.contains("=")) {
			if (guard.contains("!="))
				return counter != value_guard;
			return counter == value_guard;
		}
		
		return true;
		
	}
	
	private String launchSolver(File encoding, String option1, String option2) {
		return launchSolver(encoding, option1, option2, false);
	}

	private String launchSolver(File encoding, String option1, String option2, boolean check_error) {
		StringBuilder builder = new StringBuilder();
		try {
			process = new ProcessBuilder(DebuggerUtil.solver, encoding.getAbsolutePath(), option1, option2).start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			if (check_error) {
				BufferedReader br2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				System.out.print(check_error);
				get_unsupported(br2, encoding);
			}
	
			
			String line;
			while ((line = br.readLine()) != null) {
				builder.append(line + "\n");
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		} finally {
			process = null;
		}
		return builder.toString();
	}
	
	
	private void look_for_head(List<String> atoms, File encoding) throws IOException {
		String tmp = fileToString(encoding).split("#end")[1];
		for (String atom : atoms) {
			this.falseAtoms.add(atom);
			this.unsupported.add(atom);
		}
		String prefix = "(?<=\\s|,|:-)";
		String affix = "(?=\\s+|,|\\.)";
		for(String line : tmp.split("\n")) {
			if (!line.contains(":-"))
				continue;
			String tmp_head = line.split(":-")[0].trim();
			String tmp_body = line.split(":-")[1];
			for (String atom : atoms) {
					String regex = prefix + "" + atom + affix;

			        Pattern pattern = Pattern.compile(regex);
			        Matcher matcher = pattern.matcher(tmp_body);

			        if (matcher.find() && !this.falseAtoms.contains(tmp_head)) {
			        	this.falseAtoms.add(tmp_head);
			        	break;
			        }
			}
		}
	}
	
	
	private void get_unsupported(BufferedReader br, File encoding) {
		String line;
		List<String> atoms = new ArrayList<String>();
		boolean next = false;
		try {
			while ((line = br.readLine()) != null) {
				if (line.contains("any rule head")) {
					next = true;
					continue;
				}
				if (next) {
					next = false;
					atoms.add(line.trim());
				}
			}
			if (atoms.size() > 0)
				look_for_head(atoms, encoding);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	int query(String program) throws IOException {
		String res = launchSolver(stringToTmpFile(program), "--models=10", "--outf=2");
		JSONObject obj = new JSONObject(res);
		JSONArray arr = (JSONArray) obj.get("Call");
		JSONArray models = (JSONArray) arr.getJSONObject(0).get("Witnesses");
		int nbModels = models.length();
		for (int i = 0; i < models.length(); i++) {
			JSONArray answerset = (JSONArray) models.getJSONObject(i).get("Value");
			for (int j = 0; j < answerset.length(); j++) {
				String atom = answerset.getString(j);
				if (atom.startsWith("__debug"))
					continue;
				if (!myMap.containsKey(atom)) {
					myMap.put(atom, 0);
				}
				myMap.put(atom, myMap.get(atom) + 1);
			}
		}
		return nbModels;
	}
}