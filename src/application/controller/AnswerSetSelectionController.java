package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import application.model.debugger.CostLevel;
import application.model.debugger.Response;
import application.view.CustomFontIcon;
import application.view.SceneHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AnswerSetSelectionController {

    @FXML
    private Button backButton;


    
    @FXML
    private ProgressBar progressBar;
    
    private Justifier justifier;
    
    @FXML
    private ListView<String> allAS;


    private List<String> answerSets;

    private final ExplainAtomService justifierService = new ExplainAtomService();
    

    public void init(List<String> answerSets, Justifier justifier) {
    	this.answerSets = answerSets;
        this.justifier = justifier;
        allAS.getItems().addAll(answerSets);
    }

    @FXML
    void back() {
        SceneHandler.getInstance().backHome();
    }
    
    @FXML 
    void selection() {
    	Integer index = allAS.getSelectionModel().getSelectedIndex();
    	List<QueryAtom> answerSets;
		try {
			answerSets = justifier.retrieveAtoms(index);
	        SceneHandler.getInstance().showInspectAnswerSetWindow(answerSets, justifier);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			SceneHandler.getInstance().showErrorMessage("Error", "Something went wrong during the computation of the justification.");
		}
        
    }

    @FXML
    void initialize() {
        backButton.setGraphic(new CustomFontIcon("mdi2c-chevron-left"));
    }
}
