package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import application.model.debugger.Response;
import application.view.CustomFontIcon;
import application.view.SceneHandler;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DebuggerController {

    @FXML
    private Button backButton;

    @FXML
    private Button homeButton;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private ProgressBar progressBar;

    private Justifier justifier;

    private final ExplainAtomService justifierService = new ExplainAtomService();

    @FXML
    private Label label;

    private final TreeItem<String> explanation = new TreeItem<>("Explanation");
    private final TreeItem<String> rules = new TreeItem<>("Rules");
    private final TreeItem<String> literals = new TreeItem<>("Literals (select to explain)");
    private final TreeItem<String> facts = new TreeItem<>("Facts");

    @FXML
    void back() {
        SceneHandler.getInstance().back();
    }

    @FXML
    void home() {
        SceneHandler.getInstance().backHome();
    }

    public void init(Justifier justifier, List<Response> response) {
        this.justifier = justifier;
        backButton.setGraphic(new CustomFontIcon("mdi2c-chevron-left"));
        homeButton.setGraphic(new CustomFontIcon("mdi2h-home"));
        for(Response res : response) {
            switch (res.getValue()) {
                case 0 -> rules.getChildren().add(new TreeItem<>(res.getRule()));
                case 1 -> facts.getChildren().add(new TreeItem<>(res.getRule()));
                case 2 -> literals.getChildren().add(new TreeItem<>(res.getRule()));
            }
        }
        rules.setExpanded(true);
        literals.setExpanded(true);
        facts.setExpanded(true);
        explanation.getChildren().add(rules);
        explanation.getChildren().add(literals);
        explanation.getChildren().add(facts);
        treeView.setRoot(explanation);
        justifierService.setOnFailed(e -> SceneHandler.getInstance().showErrorMessage("Error", "Something went wrong during the computation of the justification."));
        progressBar.visibleProperty().bind(justifierService.runningProperty());
        progressBar.progressProperty().bind(justifierService.progressProperty());
    }

}
