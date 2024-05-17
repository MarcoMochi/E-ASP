package application;

import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import de.jensd.fx.glyphs.GlyphsDude;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import debugger.Debugger;
import debugger.DebuggerUtil;
import debugger.QueryAtom;
import debugger.UnsatisfiableCore;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.scene.control.CheckBox;

public class EditorController {

	@FXML
	private MenuItem esci_menu;

	@FXML
	private MenuItem about_menu;

	@FXML
	private MenuItem nuovo_menu;

	@FXML
	private TextArea areaDiTesto;

	@FXML
	private MenuItem apri_menu;

	@FXML
	private MenuItem salva_menu;
	
	@FXML
    private MenuItem solver_menu;

	@FXML
	private TabPane tabPane;

	@FXML
	private Button debug;
	
	@FXML
	private Button debugFacts;

	@FXML
	private Button reduce;	
	
	@FXML 
	private Button justify;

	@FXML
	private CheckBox check_rules;
	
	@FXML
	private CheckBox check_AS;
	
	private Stage stage;

	private HashMap<Tab, File> tab2file;

	private Debugger d;
	private UnsatisfiableCore unsatCore;

	public void init(Stage stage) {
		this.stage = stage;
		tab2file = new HashMap<Tab, File>();
		nuovo_menu.setGraphic(GlyphsDude.createIcon(MaterialDesignIcon.OPEN_IN_NEW));
		apri_menu.setGraphic(GlyphsDude.createIcon(MaterialDesignIcon.FOLDER));
		salva_menu.setGraphic(GlyphsDude.createIcon(MaterialDesignIcon.CONTENT_SAVE));
		esci_menu.setGraphic(GlyphsDude.createIcon(MaterialDesignIcon.EXIT_TO_APP));
		about_menu.setGraphic(GlyphsDude.createIcon(MaterialDesignIcon.INFORMATION));
		//debug.setGraphic(GlyphsDude.createIcon(MaterialDesignIcon.BUG));
		//reduce.setDisable(true);
	}

	public void end() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("End of debugging");
		alert.setContentText("No more actions are possible: check red lines!");
		alert.show();
	}
	
	private void internalDebug(boolean shrink, List<QueryAtom> qa, boolean onlyFacts) {
		Tab t = tabPane.getSelectionModel().getSelectedItem();
		if (t == null || !tab2file.containsKey(t)) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error in debug");
			alert.setContentText("No file is selected!");
			alert.show();
		} else {
			File f = tab2file.get(t);
			if(shrink) {
				unsatCore = d.debug(qa);
				if(unsatCore != null) {
					if(unsatCore.getLines().size() == 0) {
						String unsupported = "";
						for(QueryAtom q : qa) {
							if(q.getValue()==QueryAtom.TRUE)
								unsupported += "- " + q.getAtom() + "\n";
						}
						
						Alert alert = new Alert(AlertType.INFORMATION);
						alert.setTitle("End of debugging");
						alert.setContentText("One of the following atoms:\n" + unsupported + "cannot be supported.\n"
								+ "Check all rules where they appear in the head or add a supporting rule.");	
						alert.show();
						//reduce.setDisable(true);
						return;
					}
				}
			}
			else {
				d = new Debugger(f, onlyFacts);
				unsatCore = d.debug();				
			}
			if(unsatCore == null) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Error");
				alert.setContentText("Some went wrong during debugging. Please check if your encoding is ASPCore2.");
				alert.show();
				//reduce.setDisable(true);
			}
			@SuppressWarnings("unchecked")
			VirtualizedScrollPane<InlineCssTextArea> vs = (VirtualizedScrollPane<InlineCssTextArea>) t.getContent();
			InlineCssTextArea area = (InlineCssTextArea) vs.getContent();
			ArrayList<Integer> faultyLines = unsatCore.getLines();
			for(int i = 0; i < area.getParagraphs().size(); i++)
				area.setStyle(i, "-fx-fill: black;");
			
			String[] allLines = area.getText().split("\n");
			for (Integer i : faultyLines) {				
				area.setStyle(i - 1, "-fx-fill: red;");
			}
			
			if(!onlyFacts && !d.canBeQueried()) {				
				end();
				//reduce.setDisable(true);
			}
			else if (faultyLines.size() > 1) {
				if(!onlyFacts)
					end();
					//reduce.setDisable(false);
				else
					end();
			}
			else if (faultyLines.size() == 1) {
				//reduce.setDisable(true);
				end();
			}
			else {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("End of debugging");
				alert.setContentText("No faulty rules detected!");
				alert.show();
				reduce.setDisable(true);
			}			
		}
	} 

	private void internalDebug(boolean shrink, List<QueryAtom> qa, boolean onlyFacts, boolean justify, boolean back_flag) {
		Tab t = tabPane.getSelectionModel().getSelectedItem();
		File f = tab2file.get(t);
		if (!back_flag) {
			unsatCore = d.debug(qa, justify);
			d.addCore(unsatCore);
		}
		else
			unsatCore = d.getCore();
		
			if (unsatCore == null) {
				chooseAtom();
				return;
			}
		
			String analyzed = d.getAnalyzed();
		if (analyzed == null)
			chooseAtom();
		
		if(unsatCore != null) {
			if(unsatCore.getLines().size() == 0) {
				String unsupported = "";
				for(QueryAtom q : qa) {
					if(q.getValue()==QueryAtom.FALSE)
						unsupported += "- " + q.getAtom() + "\n";
				}
				
				
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("End of Justification");
				alert.setContentText("One of the following atoms:\n" + unsupported + "cannot be justified.\n"
						+ "Its value was assigned during the solving phase freely.");	
				alert.show();
				//reduce.setDisable(true);
				return;
			}
		}
		if(unsatCore == null) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error");
			alert.setContentText("Some went wrong during the justification. Please check if your encoding is ASPCore2.");
			alert.show();
			//reduce.setDisable(true);
		}
		Stage stage = new Stage();
		
		stage.setTitle("Supporting atoms and rules for: " +analyzed);
		stage.setWidth(600);
		stage.setHeight(300);
		stage.initModality(Modality.APPLICATION_MODAL);			
		VBox vBox = new VBox(5);
		vBox.setPadding(new Insets(3,0,0,5));
		ToggleGroup group = new ToggleGroup();
		List<String> querable = new ArrayList<String>();
		List<String> externals = new ArrayList<String>();
		externals.add(analyzed + ".");
		List<String> seen = new ArrayList<String>();
		Boolean could_justify = false;
		for (String r : unsatCore.getRules()) {
			if (seen.contains(r))
				continue;
			seen.add(r);
			
			if (unsatCore.getExplanations().size() == 1) {
				if (unsatCore.getExplanations().get(0) == "unsupported") {
					Label label = new Label("The atom is not supported by any rule.");
					//reduce.setDisable(true);
					break;
				}
			}
			
			Boolean add_box = false;
			Label label;
			if (r.contains(":-")) {
				if (r.contains("not ")) { 
					String temp = r.split("not ")[1];
					String checker = temp.substring(0,temp.length()-1);
					if (d.getDerivedAtoms().contains(checker) & !(checker.equals(analyzed) & r.equals(":- not " + checker + "."))) {
						label = new Label("Supporting Derived Atom: " + checker);
						externals.add(temp);
						add_box = true;
						could_justify = true;
					}
					else {
							label = new Label("Supporting rule: " + r);
							externals.add(r);
					}
				}
				else {
					String temp = r.split(":- ")[1];
					String checker = temp.substring(0,temp.length()-1);
					if (d.getFalseAtoms().contains(checker) && r.split(":- ")[0].length() < 1 & !(checker.equals(analyzed) & r.equals(":- " + checker + "."))) {
						label = new Label("Supporting false atom: not " + checker);
						add_box = true;
						could_justify = true;
					}
					else {
						label = new Label("Supporting rule: " + r);
						externals.add(r);
					}
				}
			}
			else {
				label = new Label("Supporting atom: " + r);
				externals.add(r);
			}
			HBox hBox = new HBox();
			hBox.setPadding(new Insets(3,0,0,5));
        	
			hBox.getChildren().add(label);

			if (add_box) { 
				String temp;
				if (r.contains("not ")) {
					temp = r.split("not ")[1];
					temp = temp.substring(0,temp.length()-1);
				} 
				else {
					temp = r.split(":- ")[1];
					temp = temp.substring(0,temp.length()-1);
				}
				querable.add(temp);
				ToggleButton tb1 = new ToggleButton("Justify");
				tb1.setToggleGroup(group);
				tb1.setId(temp);
				hBox.getChildren().add(tb1);
			}
			
			if (r.contains("#")) { 
				Pattern pattern = Pattern.compile("\\{(.*?)\\}");
				Matcher matcher = pattern.matcher(r);
		        if (matcher.find()) {
		        	Button inspect = new Button("Inspect");
		        	hBox.getChildren().add(inspect);
		    		inspect.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
		    			@Override
		    			public void handle(ActionEvent event) {	
		    				List<Object> set_atoms_message = d.generateSet(externals, r);
		    				List<HashMap<String,HashMap<String, List<String>>>> setAtoms = (List<HashMap<String, HashMap<String, List<String>>>>) set_atoms_message.get(0);
		    				String message = (String) set_atoms_message.get(1);
		    				showAtoms(setAtoms, r, message);
		    			}
		    		});			

		        }   
			}
			
			vBox.getChildren().add(hBox); 	
		}
		HBox hBoxButton = new HBox(5);
		Button back = new Button("Back");
		hBoxButton.getChildren().add(back);
		Button restart = new Button("Justify");
		if (could_justify)
			hBoxButton.getChildren().add(restart);
		// Start other core search
		//Button change = new Button("Other rule");
		//hBoxButton.getChildren().add(change);
		ScrollPane scroll = new ScrollPane();
		scroll.setContent(vBox);
		hBoxButton.setPadding(new Insets(2,0,4,4));
		vBox.getChildren().add(hBoxButton);

		Scene s = new Scene(scroll);
		restart.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {	
				
				for(String r : querable) {
					ToggleButton tb1 = (ToggleButton) s.lookup("#"+r);
					if(tb1.isSelected()) {
						for(QueryAtom q : qa) {
							if (q.getAtom().equals(r)) {
								q.setValue(QueryAtom.FALSE);
								d.addAnalyzed(q.getAtom());
							}
							else if (q.getValue() == QueryAtom.FALSE)
								q.setValue(QueryAtom.NOT_SET);
						}
					}
				stage.close();
				}

				//d = new Debugger(d.returnFile(), false, false, d.getOrder());
				internalDebug(false, qa, true, true, false);	
			}
		});
		// Handle other core search
		//change.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
		//	@Override
		//	public void handle(ActionEvent event) {
		//		for(String r : unsatCore.getRules()) {	
		//				d.setIgnore(r);
		//			}
		//	//} d.backStack();
		//		stage.close();
		//		internalDebug(false, qa, true, false, false);
		//	}
		//});			
		back.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				d.backAnalyzed();
				d.backCore();
				d.updateQuery(qa);
				stage.close();
				internalDebug(false, qa, true, false, true);
			}
		});		
		stage.setScene(s);	
		stage.show();
		
	} 
	

	private void solveToJustify() {
		Tab t = tabPane.getSelectionModel().getSelectedItem();
		if (t == null || !tab2file.containsKey(t)) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error in justify");
			alert.setContentText("No file is selected!");
			alert.show();
		} else {
			File f = tab2file.get(t);
			try	{
				if (!(check_rules.isSelected() || check_AS.isSelected())) {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Error in justify");
					alert.setContentText("Select at least one checkbox between rules and Answer Set!");
					alert.show();
					return;
				}
				d = new Debugger(f, false, true, check_rules.isSelected(), check_AS.isSelected());
				chooseAtom();
			} catch (Exception e) {
				Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("End of solving");
				alert.setContentText("There is no solution to the provided model");	
				alert.show();
				e.printStackTrace();
				return;
			}
			
				
			}
		}
	
	@FXML
	void debug(ActionEvent event) {
		List<QueryAtom> qa = new ArrayList<QueryAtom>();
		internalDebug(false, qa, false);
	}
	
	@FXML
	void debugFacts(ActionEvent event) {
		List<QueryAtom> qa = new ArrayList<QueryAtom>();
		internalDebug(false, qa, true);
	}
	
	@FXML
	void justify(ActionEvent event) {
		solveToJustify();
	}
	

	@FXML
	void chooseAtom() {
		try {
			List<QueryAtom> qa = d.populateQuery();
			List<String> querable_d = d.getDerivedAtoms();
			List<String> querable_f = d.getFalseAtoms();
			Stage stage = new Stage();
			stage.setTitle("Choose Atom to Justify");
			stage.setWidth(600);
			stage.setHeight(300);
			stage.initModality(Modality.APPLICATION_MODAL);	
			
			VBox vBox = new VBox(5);
			vBox.setPadding(new Insets(2,0,4,4));
			ToggleGroup group = new ToggleGroup();
			for (QueryAtom q : qa) {
				if (querable_d.contains(q.getAtom()) || querable_f.contains(q.getAtom()) && querable_d != null) {
					Label label;
					if (querable_d.contains(q.getAtom()))
						label = new Label("Select to see cause of " + q.getAtom() + ": ");
					else
						label = new Label("Select to see cause of not " + q.getAtom() + ": ");
					
					HBox hBox = new HBox();
				    hBox.setPadding(new Insets(2,0,4,4));
				    ToggleButton tb1 = new ToggleButton("Select");
					tb1.setToggleGroup(group);
					tb1.setId(q.getAtom());
					
					//label.setPadding(new Insets(2,0,0,5));
					hBox.getChildren().add(label);
					hBox.getChildren().add(tb1);
				    vBox.getChildren().add(hBox); 	
				}
			}
			ScrollPane scroll = new ScrollPane();
			scroll.setContent(vBox);
			
			Button confirm = new Button("Confirm");
			vBox.getChildren().add(confirm);
			Scene s = new Scene(scroll);
			stage.setScene(s);
			confirm.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {		
					for(QueryAtom q: qa) {
						if (querable_d.contains(q.getAtom()) || querable_f.contains(q.getAtom())) {
							ToggleButton tb1 = (ToggleButton) s.lookup("#"+q.getAtom());
							if(tb1.isSelected()) {
								q.setValue(QueryAtom.FALSE);
								d.addAnalyzed(q.getAtom());
							}
						}
					stage.close();
					}
				
				if (qa.isEmpty()) {
					end();
					reduce.setDisable(true);
				}
				else {
					internalDebug(true, qa, false, true, false);
				}
				}
		});	
			
			stage.showAndWait();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@FXML
	void showAtoms(List<HashMap<String, HashMap<String, List<String>>>> sets, String rule, String message) {
		Stage stage = new Stage();
		stage.setTitle(message + "for rule: " + rule);
		stage.setWidth(600);
		stage.setHeight(300);
		stage.initModality(Modality.APPLICATION_MODAL);	
        Accordion accordion = new Accordion();
        
        VBox vBox = new VBox(accordion);
        HBox hBox = new HBox(5);
        HashMap<String, HashMap<String, List<String>>> set_opt = sets.get(1);
        ToggleGroup group = new ToggleGroup();
        for (HashMap.Entry<String, HashMap<String, List<String>>> map_pairs : set_opt.entrySet()) {
        	TitledPane pane_temp = new TitledPane();
        	ListView<Object> list_set = new ListView<Object>();
        	List<Object> list_temp = new ArrayList<Object>();
        	String used_atom = map_pairs.getKey();
        	int n_set = map_pairs.getValue().size();
        	
        	for (HashMap.Entry<String, List<String>> entry : map_pairs.getValue().entrySet()) {
        		String value = entry.getKey();
        		List<String> set = entry.getValue();
        		if (set.size() == 1) {
        			list_temp.add(value + " : " + String.join(",", set));
        		}
        	}
        	ObservableList<Object> items = FXCollections.observableArrayList (list_temp);
        	list_set.setItems((ObservableList<Object>) items);
            list_set.setPrefWidth(600);
            list_set.setPrefHeight(140);
            if (used_atom.equals(""))
            	pane_temp.setText("Generated set with : " + Integer.toString(n_set) + " elements");
            else
            	pane_temp.setText("Generated set for " + used_atom + " with : " + Integer.toString(n_set) + " elements");
            pane_temp.setContent(list_set);
            if (rule.contains("<") || rule.contains(">")) {
            	ToggleButton tb1;
            	if (used_atom.equals("")) {
            		tb1 = new ToggleButton("Show all atoms in the set");
            	} else {
            		tb1 = new ToggleButton("Show all atoms in the set for " + used_atom);
            	}
				tb1.setToggleGroup(group);
				tb1.setId(map_pairs.getKey());
				
				hBox.getChildren().add(tb1);
            }
		    accordion.getPanes().add(pane_temp);
            
            	
        }

        vBox.getChildren().add(hBox); 
        HBox hBoxButton = new HBox();
        vBox.setPadding(new Insets(4,0,4,4));
        Button inspectAll = new Button("Inspect All");
		
        if (rule.contains("<") || rule.contains(">")) {
        	 hBoxButton.getChildren().add(inspectAll);
			vBox.getChildren().add(inspectAll);
        }
		//ScrollPane scroll = new ScrollPane();
		//scroll.setContent(vBox);
        Scene scene = new Scene(vBox);

		stage.setScene(scene);

		inspectAll.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {		
				ToggleButton tb = (ToggleButton) group.getSelectedToggle();
				if (tb != null) {
					String key = tb.getId();
					showAllAtoms(key, sets.get(0), rule);
				}
			}		
		});	

        stage.show();
	}
	
	@FXML
	void showAllAtoms(String key, HashMap<String, HashMap<String, List<String>>> sets, String rule) {
		Stage stage = new Stage();
		stage.setTitle("All atoms in the Aggregate in the rule: " + rule);
		stage.setWidth(600);
		stage.setHeight(300);
        stage.show();
        
        VBox vBox = new VBox();
	    
        HashMap<String, List<String>> map_values = sets.get(key);
        for (Entry<String, List<String>> entry : map_values.entrySet()) {
        	Label label = new Label(entry.getKey() + " : " + String.join(",", entry.getValue()));
    		
        	HBox hBox = new HBox();
    		hBox.setPadding(new Insets(2,0,4,4));
    	    hBox.getChildren().add(label);
    	    vBox.getChildren().add(hBox); 	
        }
        
        
        
        ScrollPane scroll = new ScrollPane();
		scroll.setContent(vBox);
        Scene scene = new Scene(scroll);

        stage.setScene(scene);

        stage.show();
	}
	
	@FXML
	void reduceFaultyRules(ActionEvent event) {
		try {
			List<QueryAtom> qa = d.computeQuery(unsatCore);
			Stage stage = new Stage();
			stage.setTitle("Query");
			stage.setWidth(600);
			stage.setHeight(300);
			stage.initModality(Modality.APPLICATION_MODAL);			
			VBox vBox = new VBox();
			for (QueryAtom q : qa) {
				Label label = new Label("Select a value for atom " + q.getAtom() + ": ");
			    HBox hBox = new HBox();
			    hBox.setSpacing(2);
			    ToggleButton tb1 = new ToggleButton("T");
			    ToggleButton tb2 = new ToggleButton("F");
				ToggleButton tb3 = new ToggleButton("U");
				ToggleGroup group = new ToggleGroup();
				tb1.setToggleGroup(group);
				tb2.setToggleGroup(group);
				tb3.setToggleGroup(group);
				tb1.setId(q.getAtom()+"tb1");
				tb2.setId(q.getAtom()+"tb2");
				tb3.setId(q.getAtom()+"tb3");
				
				hBox.getChildren().add(label);
				hBox.getChildren().add(tb1);
			    hBox.getChildren().add(tb2);
			    hBox.getChildren().add(tb3);
			    
			    vBox.getChildren().add(hBox); 			    
			}
			Button confirm = new Button("Confirm");
			vBox.getChildren().add(confirm);
			Scene s = new Scene(vBox);
			stage.setScene(s);			
			confirm.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					for(QueryAtom q : qa) {
						ToggleButton tb1 = (ToggleButton) s.lookup("#"+q.getAtom()+"tb1");
						ToggleButton tb2 = (ToggleButton) s.lookup("#"+q.getAtom()+"tb2");
						if(tb1.isSelected())
							q.setValue(QueryAtom.TRUE);					
						else if(tb2.isSelected())
							q.setValue(QueryAtom.FALSE);
						else
							q.setValue(QueryAtom.UNDEFINED);
					}
					stage.close();
				}
			});			
			stage.showAndWait();
			if(qa.isEmpty()) {
				end();
				reduce.setDisable(true);
			}
			else {
				internalDebug(true, qa, false);				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	void nuovoMenu(ActionEvent event) {
		nuovoTab("Untitled");
	}

	@FXML
	void apriMenu(ActionEvent event) {
		FileChooser f = new FileChooser();
		File chosenFile = f.showOpenDialog(stage);
		if (chosenFile != null && !chosenFile.isDirectory()) {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(chosenFile));
				Tab t = nuovoTab(chosenFile.getName());
				@SuppressWarnings("unchecked")
				VirtualizedScrollPane<InlineCssTextArea> vs = (VirtualizedScrollPane<InlineCssTextArea>) t.getContent();
				InlineCssTextArea area = (InlineCssTextArea) vs.getContent();
				while (reader.ready()) {
					area.appendText(reader.readLine() + System.lineSeparator());
				}
				reader.close();
				tab2file.put(t, chosenFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	void salvaMenu(ActionEvent event) {
		salva();
	}

	private void salva() {
		Tab t = tabPane.getSelectionModel().getSelectedItem();
		if(t == null)
			return;
		File chosenFile;
		if (!tab2file.containsKey(t)) {
			FileChooser f = new FileChooser();
			chosenFile = f.showSaveDialog(stage);
			if (chosenFile == null)
				return;
			tab2file.put(t, chosenFile);
		} else {
			chosenFile = tab2file.get(t);
		}
		t.setText(chosenFile.getName());
		try {
			BufferedWriter bf = new BufferedWriter(new FileWriter(chosenFile));
			@SuppressWarnings("unchecked")
			VirtualizedScrollPane<InlineCssTextArea> vs = (VirtualizedScrollPane<InlineCssTextArea>) t.getContent();
			InlineCssTextArea area = (InlineCssTextArea) vs.getContent();
			bf.append(area.getText());
			bf.flush();
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	void esciMenu(ActionEvent event) {
		stage.close();
	}
	
	@FXML
    void solverMenu(ActionEvent event) {		
		Stage stage = new Stage();
		stage.setTitle("Solver path");
		stage.setWidth(300);
		stage.setHeight(100);
		stage.initModality(Modality.APPLICATION_MODAL);			
		Label label = new Label("Solver path: ");
		Label currentPath = new Label(DebuggerUtil.solver);
		Button change = new Button("Change");
	    HBox hBox = new HBox();
	    hBox.setSpacing(4);
	    hBox.getChildren().add(label);
	    hBox.getChildren().add(currentPath);
	    hBox.getChildren().add(change);
	    change.addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				FileChooser f = new FileChooser();
				File chosenFile = f.showOpenDialog(stage);
				if (chosenFile != null && !chosenFile.isDirectory()) {
					DebuggerUtil.solver = chosenFile.getAbsolutePath();
					DebuggerUtil.writeSettings();
					stage.close();
				}				
			}
		});
		Scene s = new Scene(hBox);
		stage.setScene(s);			
		stage.showAndWait();
    }

	@FXML
	void aboutMenu(ActionEvent event) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About");
		alert.setHeaderText(null);
		alert.setContentText("E-ASP");
		alert.showAndWait();
	}

	private Tab nuovoTab(String nome) {
		Tab t = new Tab(nome);
		tabPane.getTabs().add(t);
		tabPane.getSelectionModel().select(t);
		InlineCssTextArea area = new InlineCssTextArea();
		VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(area);
		area.setParagraphGraphicFactory(LineNumberFactory.get(area));
		t.setContent(vsPane);
		area.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				if (ke.getCode() == KeyCode.S && (ke.isControlDown() || ke.isMetaDown())) {
					salva();
				} else if (!t.getText().endsWith("*"))
					t.setText(t.getText() + "*");
			}
		});
		return t;
	}

}
