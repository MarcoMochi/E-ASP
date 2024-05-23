package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import application.model.debugger.Response;
import application.view.CustomFontIcon;
import application.view.SceneHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class AnswerSetInspectionController {

    @FXML
    private Button backButton;

    @FXML
    private ListView<QueryAtom> allAtoms;

    @FXML
    private ProgressBar progressBar;

    private Justifier justifier;

    private final ExplainAtomService justifierService = new ExplainAtomService();

    private QueryAtom queryAtom;

    private List<QueryAtom> answerSet;

    @FXML
    void explainAtom() {
        queryAtom = allAtoms.getSelectionModel().getSelectedItem();
        justifierService.setParameters(justifier, new ArrayList<>(), queryAtom);
        justifierService.restart();
    }

    public void init(List<QueryAtom> answerSet, Justifier justifier) {
        allAtoms.getItems().addAll(answerSet);
        this.answerSet = answerSet;
        this.justifier = justifier;
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
    }
}
