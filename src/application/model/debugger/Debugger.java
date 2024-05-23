package application.model.debugger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;


public class Debugger {

	private Process process;
	private List<String> debugAtoms;
	private List<String> initialFacts;
	private List<String> derivedAtoms;
	private List<String> falseAtoms;
	private List<String> rules;
	private List<String> order;
	private QueryAtom analyzed;
	boolean onlyFacts;
	private List<String> rulesIgnored;
	private List<QueryAtom> order_analyzed;
	private List<UnsatisfiableCore> stackCore;
	private Boolean debug_rules;
	private Boolean debug_AS;
	private List<String> unsupported;

	public Debugger(boolean deb_rules, boolean deb_AS) {
		this.rulesIgnored = new ArrayList<String>();
		this.order_analyzed = new ArrayList<QueryAtom>();
		this.stackCore = new ArrayList<UnsatisfiableCore>();
		this.debug_rules = deb_rules;
		this.debug_AS = deb_AS;
		this.unsupported = new ArrayList<String>();
	}	
	
	//try {
	//		getFacts(f);
	//		computeAtoms();
	//	} 
	//	catch (IOException e) {
	//		throw e;
	//	}
	//}

	public void stopDebug() {
		if (process != null)
			process.destroy();
	}
	
	
	void computeAtoms(String program) throws IOException {
		derivedAtoms = new ArrayList<String>();
		falseAtoms = new ArrayList<String>();
		File helper = new File(DebuggerUtil.helper);
		String tmp = fileToString(helper);
		tmp = tmp + program;
		String output = launchSolver(tmp, "--models=1", "--outf=2", true);
		File tmpFile = new File(".tmp_file2");
		String output_info = fileToString(tmpFile); 
		tmpFile.deleteOnExit();
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

		 
	public UnsatisfiableCore debug(QueryAtom atom, List<QueryAtom> chain, List<QueryAtom> queries, String program) {				
		try {
			program = addDerived(program, atom, chain, queries);
			program = setRulesForOrder(program, atom);
			String extendedProgram = extendProgram(program, atom);
			return computeMinimalCore(extendedProgram, atom);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private String setRulesForOrder(String program, QueryAtom atom) {
		
		boolean ignore = false;
		List<String> atoms = new ArrayList<String>();
		List<String> order_loc = getOrder();
		if (order_loc.contains(atom.getAtom())) {
			for(String i : order_loc) {
				if (i.equals(atom.getAtom())) {
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
			for (String tmp_atom : atoms) {
				if (line.contains(tmp_atom)) {
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
	

	private String addDerived(String program, QueryAtom atom, List<QueryAtom> chain, List<QueryAtom> queries) {
		StringBuilder builder = new StringBuilder();
		builder.append("%Add Answer Set\n");
		for (QueryAtom q : queries) {
			if (q.equals(atom)) {
				if (q.getValue() == QueryAtom.FALSE) 
					builder.append(":- not " + q.getAtom() + ".\n");
				else
					builder.append(":- " + q.getAtom() + ".\n");
			}
			else if (chain.contains(q)) {
				if (q.getValue() == QueryAtom.FALSE) 
					builder.append(":- " + q.getAtom() + ".@ignore\n");
				else
					builder.append(":- not " + q.getAtom() + ".@ignore\n");
			}
			else {
				if (q.getValue() == QueryAtom.FALSE) 
					builder.append(":- " + q.getAtom() + ".\n");
				else
					builder.append(":- not " + q.getAtom() + ".\n");
			}
		}
		return program + builder.toString();
	}

	
	
	public List<QueryAtom> populateQuery() {
		ArrayList<QueryAtom> qa = new ArrayList<QueryAtom>();
		//for (String i : initialFacts) {
		//	qa.add(new QueryAtom(i, QueryAtom.TRUE));
		//}
		if (derivedAtoms != null) {
			for (String i : derivedAtoms) {
				qa.add(new QueryAtom(i, QueryAtom.TRUE));
			}
		}
		for (String i : falseAtoms) {
			qa.add(new QueryAtom(i, QueryAtom.FALSE));
		}
		//for (String i : rules) {
		//	qa.add(new QueryAtom(i, QueryAtom.NOT_SET));
		//}
		
		
		return qa;
	}
	
	public List<QueryAtom> selectableQuery(List<QueryAtom> qa) {
		List<QueryAtom> temp_qa = new ArrayList<QueryAtom>();
		for (QueryAtom q : qa) {
			if (this.derivedAtoms.contains(q.getAtom()) || this.falseAtoms.contains(q.getAtom()) && this.derivedAtoms != null) {
				temp_qa.add(q);
			}
		}
		
		return temp_qa;
	}
	
	public List<String> getDerivedAtoms() {
		return derivedAtoms;
	}
	
	public List<String> getFalseAtoms() {
		return falseAtoms;
	}

	private File stringToTmpFile(String program) throws IOException {
		File tempFile = File.createTempFile("e-asp-", ".tmp.asp");
		FileWriter fileWriter = new FileWriter(tempFile, true);
		fileWriter.write(program);
		fileWriter.close();
		tempFile.deleteOnExit();
		return tempFile;
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
	
	public void getFacts(String program) throws IOException {
		StringBuilder tempProgram = new StringBuilder();
		initialFacts = new ArrayList<String>();
		rules = new ArrayList<String>();
		String output = launchSolver(program.toString(),  "--mode=gringo", "--text");
		for(String line : program.split("\n")) {
			if (!line.contains(":-") && !line.contains(":~") && !line.contains("{") && line.length() > 0) {
				tempProgram.append(line);
			} else if (line.contains(":-"))  {
				rules.add(line.substring(0, line.trim().length()-1));
			}
		}
		if (tempProgram.toString().length() > 0) {
			output = launchSolver(tempProgram.toString(),  "--mode=gringo", "--text");
			for (String atom : output.split("\n")) {
				if (atom.length() > 0)
					initialFacts.add(atom.substring(0, atom.length()-1));
			}
		}
	}	


	private String extendProgram(String p, QueryAtom atom) {
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
			if (!start_AS) {
				if(debug_rules) {
			
					String deb = "";
					if (line.contains(":-")) {
						if (line.contains("#count") || line.contains("#sum")) 
							deb = "__debug(\"" + line_parsed + "\",3," + cont + ")";
						else
							deb = "__debug(\"" + line_parsed + "\",0," + cont + ")";
						line = line.substring(0, line.length() - 1) + ", not " + deb + ".";
						debugAtoms.add(deb);
					} else if (line.contains(".")) {
						deb = "__debug(\"" + line_parsed + "\",1," + cont + ")";
						line = line.substring(0, line.length() - 1) + ":- not " + deb + ".";
						debugAtoms.add(deb);
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
				
				String sup = "__support(\"" + line_parsed + "\",0," + cont + ")";
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
			
					String deb = "__debug(\"" + atom_tmp + "\",2," + cont + ")";
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
				
			if (!info.equalsIgnoreCase("% no description"))
				info = "% no description";
		}
		
		if (atom.getValue() == QueryAtom.TRUE)
			builder.append(":- " + atom.getAtom() + ".\n");
		else
			builder.append(":- not " + atom.getAtom() + ".\n");
		
		return builder.toString();
	}
	
	private boolean checkCoherence(String extendedProgram, List<String> core) throws IOException {
		String tmp = extendedProgram;
		for (int i = 0; i < core.size(); i++) {
			if (!core.get(i).contains("show"))
				tmp += ":- " + core.get(i) + ".\n";
		}		
		return (!isIncoherent(tmp, "--outf=1", "--keep-facts"));			
	}

	private UnsatisfiableCore computeMinimalCore(String extendedProgram, QueryAtom atom) throws Exception {
		UnsatisfiableCore unsatCore = new UnsatisfiableCore();	
		if (this.unsupported.contains(atom.getAtom())) {
			unsatCore.addRule("No rules with atom in the head", 0);
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
			if (!isIncoherent(tmp, "--outf=1", "--keep-facts"))
				minimalCore.add(last);
		}
		
		for (String s : minimalCore) {
			Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
			Matcher m = pattern.matcher(s);
			if (m.find()) {
				unsatCore.addRule(m.group(1).replaceAll("\"", "").replace("\\", ""), Integer.parseInt(m.group(2)));
			}
		}
		
		for (String s : minimalCore) {
			Pattern pattern = Pattern.compile("__support\\((\".*\"),(.*),(.*)\\)");
			Matcher m = pattern.matcher(s);
			if (m.find()) {
				
				//System.out.print("Got supported: " + m.group(1).replaceAll("\"", ""));
				
				if (this.debug_rules) {
					List<String> head_rules = searchHead(m.group(1).replaceAll("\"", ""), extendedProgram);
					for (String head : head_rules) {
						if (head.contains("#count") || head.contains("#sum"))
							unsatCore.addRule(head, 3);
						else if (!head.contains(":-"))
							unsatCore.addRule(head, 1);
						else
							unsatCore.addRule(head, 0);
					}
				}
				
				if (this.debug_AS) {
					if (this.unsupported.contains(m.group(1).replaceAll("\"", "").replace(":-", "").replace(".", "").trim())) {
						unsatCore.addRule(m.group(1).replaceAll("\"", "").replace("\\", ""), 2);
						continue;
					}
					
					if (!m.group(1).replaceAll("\"", "").replace(":-", "").replace(" not ", "").replace(".", "").trim().equals(atom.getAtom())) {
						unsatCore.addRule(m.group(1).replaceAll("\"", "").replace("\\", ""), 2);
							continue;
						}
					}
				
				}
			}
		
		
		return unsatCore;
	}
	
	
	private List<String> searchHead(String atom, String program) {
		ArrayList<String> rules = new ArrayList<String>(); 
		String head = atom.split(":-")[1].split(Pattern.quote("("))[0].replace("not", "").replace(".", "").trim();
		int arity;
		if (atom.split(":-")[1].contains("("))
			arity = getArity(atom.split(":-")[1].split(Pattern.quote("("))[1]);
		else
			arity = getArity(atom.split(":-")[1]);
		
		for(String line : program.split("\n")) {
			if (line.startsWith("{__debug") || line.contains("__support"))
				continue;
			
			if (line.contains(":-") && line.split(":-")[0].length() > 0) {	
				String tmp_head = line.split(":-")[0].split(Pattern.quote("("))[0].trim();
				int tmp_arity;
				
				if (line.split(":-")[0].contains("|")) {
					for (String tmp_poss : line.split(":-")[0].split(Pattern.quote("|"))) {
						tmp_head = tmp_poss.split(":")[0].split(Pattern.quote("("))[0].trim();
						if (tmp_head.contains("("))
							tmp_arity = getArity(tmp_poss.split(":")[0].split(Pattern.quote("("))[1]);
						else
							tmp_arity = getArity(tmp_poss.split(":")[0]);
						
						if (!tmp_poss.contains(":")) {
							
							if (atom.equals(":- not " + tmp_poss.trim() + ".")) {
								Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
								Matcher m = pattern.matcher(line);
								if (m.find())
									rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
								
							}
						} else if (head.equals(tmp_head) && arity == tmp_arity) {
							Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
							Matcher m = pattern.matcher(line);
							if (m.find()) {
								rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
							}
						}	
					}
					
				} else if (line.split(":-")[0].contains("{")) {
					for (String tmp_poss : line.split(":-")[0].split(";")) {
						tmp_head = tmp_poss.split(":")[0].replace("{", "").replace("{", "").split(Pattern.quote("("))[0].trim();
						if (tmp_head.contains("("))
							tmp_arity = getArity(tmp_poss.split(":")[0].split(Pattern.quote("("))[1]);
						else
							tmp_arity = getArity(tmp_poss.split(":")[0]);
						
						if (!tmp_poss.contains(":")) {
							
							if (atom.equals(":- not " + tmp_poss.trim().replace("{", "").replace("}", "") + ".")) {
								Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
								Matcher m = pattern.matcher(line);
								if (m.find())
									rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
								
							}
						} else if (head.equals(tmp_head) && arity == tmp_arity) {
							Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
							Matcher m = pattern.matcher(line);
							if (m.find()) {
								rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
							}
						}
					}
				}
				else {
					if (line.split(":-")[0].contains("("))
						tmp_arity = getArity(line.split(":-")[0].split(Pattern.quote("("))[1]);
					else
						tmp_arity = getArity(line.split(":-")[0]);
					
					if (head.equals(tmp_head) && arity == tmp_arity) {
						Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
						Matcher m = pattern.matcher(line);
						if (m.find()) {
							rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
						}
					}
					
				}
				
			} else if (line.contains("|")) {
				for (String tmp_poss : line.split(Pattern.quote("|"))) {
					String tmp_head = tmp_poss.split(":")[0].split(Pattern.quote("("))[0].trim();
					int tmp_arity;
					if (tmp_head.contains("("))
						tmp_arity = getArity(tmp_poss.split(":")[0].split(Pattern.quote("("))[1]);
					else
						tmp_arity = getArity(tmp_poss.split(":")[0]);
					
					if (!tmp_poss.contains(":")) {
						
						if (atom.equals(":- not " + tmp_poss.trim() + ".")) {
							Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
							Matcher m = pattern.matcher(line);
							if (m.find())
								rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
							
						}
					} else if (head.equals(tmp_head) && arity == tmp_arity) {
						Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
						Matcher m = pattern.matcher(line);
						if (m.find()) {
							rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
						}
					}
				}
			} else if (line.contains("{")) {
					for (String tmp_poss : line.split(";")) {
						String tmp_head = tmp_poss.split(":")[0].split(Pattern.quote("("))[0].trim();
						int tmp_arity;
						if (tmp_head.contains("("))
							tmp_arity = getArity(tmp_poss.split(":")[0].split(Pattern.quote("("))[1]);
						else
							tmp_arity = getArity(tmp_poss.split(":")[0]);
						
						if (!tmp_poss.contains(":")) {
							
							if (atom.equals(":- not " + tmp_poss.trim().replace("{", "").replace("{", "") + ".")) {
								Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
								Matcher m = pattern.matcher(line);
								if (m.find())
									rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
								
							}
						} else if (head.equals(tmp_head) && arity == tmp_arity) {
							Pattern pattern = Pattern.compile("__debug\\((\".*\"),(.*),(.*)\\)");
							Matcher m = pattern.matcher(line);
							if (m.find()) {
								rules.add(m.group(1).replaceAll("\"", "").replace("\\", ""));
							}
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
	
	
	private boolean isIncoherent(String encoding, String option1, String option2) throws IOException {
		File tempFile = stringToTmpFile(encoding);
		process = new ProcessBuilder(DebuggerUtil.solver, tempFile.getAbsolutePath(), option1, option2).start();
		
		while (process.isAlive()) {
		}
		int exit_code = process.exitValue();
		process = null;
		
		return exit_code == 20;
	}
	
	public void setIgnore(String rule) {
		this.rulesIgnored.add(rule);
	}
	
	public QueryAtom backAnalyzed() {
		return order_analyzed.remove(order_analyzed.size()-1);
	}
	
	public QueryAtom getAnalyzed() {
		if (order_analyzed.size() > 0)
			return this.order_analyzed.get(order_analyzed.size()-1);
		return null;
	}
	
	public void addAnalyzed(QueryAtom atom) {
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
		
		QueryAtom new_analyzed = this.getAnalyzed();
		if (new_analyzed == null)
			return;
		
		for(QueryAtom q : queries) {
			if (!q.equals(new_analyzed))
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
		
		
		String output = launchSolver(builder.toString(),  "--mode=gringo", "--text");
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
						message = inspectCount(optSet, outside.toString().replace(to_delete, ""), entry, to_delete, internal);
						}
				}
				else if (aggregate.contains("#sum")) {
					if (to_delete.contains(">") || to_delete.contains("<")) {
						HashMap<String, List<String>> entry = totalSet.get(outside.toString().replace(to_delete, ""));
						message = inspectSum(optSet, outside.toString().replace(to_delete, ""), entry, to_delete, internal);
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
			message = "Showing all the positive and negative atoms ";
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
			if (!found & temp_body.contains(this.analyzed.getAtom()))
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
			if ((less && counter <= value_guard) || (!less && counter < (value_guard + slack))) {
				for (Entry<String, List<String>> mapping : entry.entrySet()) {
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
	
	private String inspectCount(HashMap<String,HashMap<String, List<String>>> optSet, String key, HashMap<String, List<String>> entry, String guard, Boolean truth) {
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(guard);
		Integer value_guard = 0;
		if (m.find()) {
		  value_guard = Integer.parseInt(m.group(0));
		}
		//boolean truth = check_truth(entry, guard, true);
		
		if (!this.derivedAtoms.contains(this.analyzed.getAtom())) {
			// The sign are inverted due to the way gringo show the aggregates
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
					return "Showing the first " + value_guard.toString() + " - 1 false atoms "; 
				} 
			}
		 else {
			 if (guard.contains("<=")) {
					HashMap<String, List<String>> set = findFalseAggUntil(entry, value_guard, 1, false);
					optSet.put(key, set);
					return "Showing the first " + value_guard.toString() + " false atoms causing conflict "; 
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
	
	private String inspectSum(HashMap<String,HashMap<String, List<String>>> optSet, String key, HashMap<String, List<String>> entry, String guard, Boolean truth) {
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(guard);
		Integer value_guard = 0;
		if (m.find()) {
		  value_guard = Integer.parseInt(m.group(0));
		}
		int counter = 0;
		//boolean truth = check_truth(entry, guard, true);
		Integer order = 0; 
		if (!this.derivedAtoms.contains(this.analyzed.getAtom())) {
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
	
	private String launchSolver(String encoding, String option1, String option2) {
		return launchSolver(encoding, option1, option2, false);
	}

	private String launchSolver(String encoding, String option1, String option2, boolean check_error) {
		StringBuilder builder = new StringBuilder();
		try {
			File tempFile = stringToTmpFile(encoding);
			
			process = new ProcessBuilder(DebuggerUtil.solver, tempFile.getAbsolutePath(), option1, option2).start();
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			if (check_error) {
				BufferedReader br2 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
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
	
	
	private void look_for_head(List<String> atoms, String encoding) throws IOException {
		String tmp = encoding.split("#end")[1];
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

			        if (matcher.find() && !this.falseAtoms.contains(tmp_head) && !this.initialFacts.contains(tmp_head)) {
			        	this.falseAtoms.add(tmp_head);
			        	break;
			        }
			}
		}
	}
	
	
	private void get_unsupported(BufferedReader br, String encoding) {
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

}