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

public class ExplanationController {

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

    private QueryAtom queryAtom;

    private List<QueryAtom> chain;

    private List<QueryAtom> answerSet;

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

    @FXML
    void explainAtom() {
        if(treeView.getSelectionModel().getSelectedItem() != null) {
            String atom = treeView.getSelectionModel().getSelectedItem().getValue();
            queryAtom = justifier.deriveQueryAtom(atom);
            justifierService.setParameters(justifier, chain, queryAtom);
            justifierService.restart();
        }
    }

    public void init(Justifier justifier, List<QueryAtom> answerSet, List<QueryAtom> chain, List<Response> response) {
        label.setText("Explanation chain: " + chain);
        this.justifier = justifier;
        this.chain = chain;
        this.answerSet = answerSet;
        backButton.setGraphic(new CustomFontIcon("mdi2c-chevron-left"));
        homeButton.setGraphic(new CustomFontIcon("mdi2h-home"));
        for(Response res : response) {
            switch (res.getValue()) {
                case 0 -> rules.getChildren().add(new TreeItem<>(res.getRule()));
                case 1 -> facts.getChildren().add(new TreeItem<>(res.getRule()));
                case 2 -> literals.getChildren().add(new TreeItem<>(res.getRule()));
                case 3 -> {
                    ExpandAggregateTask task = getExpandAggregateTask(justifier, res);
                    new Thread(task).start();
                }
            }
        }
        rules.setExpanded(true);
        literals.setExpanded(true);
        facts.setExpanded(true);
        treeView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldElement, newElement) -> {
                    if(newElement == rules || newElement == literals || newElement == facts)
                        Platform.runLater(() -> treeView.getSelectionModel().select(oldElement));
                    if(rules.getChildren().contains(newElement) || facts.getChildren().contains(newElement))
                        Platform.runLater(() -> treeView.getSelectionModel().select(oldElement));
                }
        );
        explanation.getChildren().add(rules);
        explanation.getChildren().add(literals);
        explanation.getChildren().add(facts);
        treeView.setRoot(explanation);
        justifierService.setOnSucceeded(e -> {
            if(!chain.contains(queryAtom)) {
                @SuppressWarnings("unchecked")
                List<Response> resp = (List<Response>) e.getSource().getValue();
                List<QueryAtom> newChain = new ArrayList<>(chain);
                newChain.add(queryAtom);
                SceneHandler.getInstance().showExplanationWindow(justifier, answerSet, newChain, resp);
            }
        });
        justifierService.setOnFailed(e -> SceneHandler.getInstance().showErrorMessage("Error", "Something went wrong during the computation of the justification."));
        progressBar.visibleProperty().bind(justifierService.runningProperty());
        progressBar.progressProperty().bind(justifierService.progressProperty());
    }

    private ExpandAggregateTask getExpandAggregateTask(Justifier justifier, Response res) {
        ExpandAggregateTask task = new ExpandAggregateTask(justifier, res.getRule(), answerSet);
        TreeItem<String> ruleWithAggregate = new TreeItem<>(res.getRule());
        task.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked")
            List<Map<String, List<String>>> aggregates = (List<Map<String, List<String>>>) e.getSource().getValue();
            for(Map<String, List<String>> map : aggregates) {
                for(String key : map.keySet()) {
                    TreeItem<String> subtree = new TreeItem<>(key);
                    ruleWithAggregate.getChildren().add(subtree);
                    for(String value : map.get(key)) {
                        subtree.getChildren().add(new TreeItem<>(value));
                    }
                }
            }
        });
        rules.getChildren().add(ruleWithAggregate);
        return task;
    }

}
