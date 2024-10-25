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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class AnswerSetInspectionController {

    @FXML
    private Button backButton;

    @FXML
    private Button optimalityButton;
    
    @FXML
    private ListView<QueryAtom> allAtoms;
    
    @FXML
    private ListView<CostLevel> allLevels;
    
    @FXML
    private Label textLevels;
    
    @FXML
    private ProgressBar progressBar;
    
    private Justifier justifier;

    private final ExplainAtomService justifierService = new ExplainAtomService();
    
    private final ExplainCostService justifierCostService = new ExplainCostService();

    private QueryAtom queryAtom;
    
    private CostLevel level;

    private List<QueryAtom> answerSet;

    @FXML
    void explainAtom() {
        queryAtom = allAtoms.getSelectionModel().getSelectedItem();
        justifierService.setParameters(justifier, new ArrayList<>(), queryAtom, false);
        justifierService.restart();
    }
    
    @FXML
    void explainOptimality() {
        level = allLevels.getSelectionModel().getSelectedItem();
        justifierCostService.setParameters(justifier, level, true);
        justifierCostService.restart();
    }

    public void init(List<QueryAtom> answerSet, Justifier justifier) {
    	this.answerSet = answerSet;
        this.justifier = justifier;
        
        allAtoms.getItems().addAll(answerSet);
        if (this.justifier.optProblem()) {
        	List<CostLevel> tmpCosts = this.justifier.requestCostLevel();
        	allLevels.getItems().addAll(tmpCosts);
        }
        else {
        	textLevels.setVisible(false);
        	optimalityButton.setVisible(false);
        }
    }

    @FXML
    void back() {
        SceneHandler.getInstance().backHome();
    }

    @FXML
    void initialize() {
        backButton.setGraphic(new CustomFontIcon("mdi2c-chevron-left"));
        justifierService.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            List<Response> response = (List<Response>) e.getSource().getValue();
            List<QueryAtom> chain = new ArrayList<>();
            chain.add(queryAtom);
            SceneHandler.getInstance().showExplanationWindow(justifier, answerSet, chain, response);
        });
        justifierService.setOnFailed(e -> SceneHandler.getInstance().showErrorMessage("Error", "Something went wrong during the computation of the justification."));
        progressBar.visibleProperty().bind(justifierService.runningProperty());
        progressBar.progressProperty().bind(justifierService.progressProperty());
        
        justifierCostService.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            List<Response> response = (List<Response>) e.getSource().getValue();
            SceneHandler.getInstance().showCostWindow(justifier, answerSet, response);
        });
        justifierCostService.setOnFailed(e -> SceneHandler.getInstance().showErrorMessage("Error", "Something went wrong during the computation of the justification of the cost."));

        progressBar.visibleProperty().bind(justifierCostService.runningProperty());
        progressBar.progressProperty().bind(justifierCostService.progressProperty());
    }
}
