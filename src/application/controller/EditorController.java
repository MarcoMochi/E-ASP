package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import application.view.CustomFontIcon;
import application.view.FileMenu;
import application.view.SceneHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class EditorController {

    @FXML
    private MenuBar menuBar;

    @FXML
    private MenuItem settingsMenuItem;

    @FXML
    private MenuItem aboutMenuItem;

    @FXML
    private MenuItem exitMenuItem;

    @FXML
    private MenuItem newMenuItem;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem saveMenuItem;

    @FXML
    private TabPane tabPane;

    @FXML
    private CheckBox checkRules;

    @FXML
    private CheckBox checkLiterals;

    @FXML
    private ProgressBar progressBar;

    private HashMap<Tab, File> tab2file;

    private final OpenFileService openFileService = new OpenFileService();

    private final ComputeFirstAnswerSetService computeFirstAnswerSetService = new ComputeFirstAnswerSetService();

    @FXML
    void justify() {
        Tab t = tabPane.getSelectionModel().getSelectedItem();
        if (t == null) {
            SceneHandler.getInstance().showErrorMessage("Error", "Please create a file!");
        } else {
            try {
                if (!(checkRules.isSelected() || checkLiterals.isSelected())) {
                    SceneHandler.getInstance().showErrorMessage("Error", "Select debugging mode between rules and/or literals!");
                    return;
                }

                @SuppressWarnings("unchecked") VirtualizedScrollPane<InlineCssTextArea> vs = (VirtualizedScrollPane<InlineCssTextArea>) t.getContent();
                InlineCssTextArea area = vs.getContent();
                Justifier justifier = new Justifier(area.getText(), checkRules.isSelected(), checkLiterals.isSelected());
                computeFirstAnswerSetService.setJustifier(justifier);
                computeFirstAnswerSetService.setOnSucceeded(e -> {
                    @SuppressWarnings("unchecked")
                    List<QueryAtom> answerSet = (List<QueryAtom>) e.getSource().getValue();
                    SceneHandler.getInstance().showInspectAnswerSetWindow(answerSet, justifier);
                });
                computeFirstAnswerSetService.setOnFailed(e -> SceneHandler.getInstance().showErrorMessage("Error", "Error while computing first answer set."));
                computeFirstAnswerSetService.restart();
            } catch (Exception e) {
                SceneHandler.getInstance().showInfoMessage("End of solving", "There is no solution to the provided model");
            }
        }
    }

    @FXML
    void newMenu() {
        createNewTab("Untitled");
    }

    @FXML
    void openMenu() {
        File chosenFile = FileMenu.openFileChooser(SceneHandler.getInstance().getPrimaryStage());
        if (chosenFile != null) {
            openFileService.setChosenFile(chosenFile);
            openFileService.restart();
        }
    }

    @FXML
    void saveMenu() {
        save();
    }

    private void save() {
        Tab t = tabPane.getSelectionModel().getSelectedItem();
        if (t == null) return;
        File chosenFile;
        if (!tab2file.containsKey(t)) {
            chosenFile = FileMenu.saveFileChooser(SceneHandler.getInstance().getPrimaryStage());
            if (chosenFile == null) return;
            tab2file.put(t, chosenFile);
        } else {
            chosenFile = tab2file.get(t);
        }
        t.setText(chosenFile.getName());
        new Thread(() -> {
            try {
                BufferedWriter bf = new BufferedWriter(new FileWriter(chosenFile));
                @SuppressWarnings("unchecked") VirtualizedScrollPane<InlineCssTextArea> vs = (VirtualizedScrollPane<InlineCssTextArea>) t.getContent();
                InlineCssTextArea area = vs.getContent();
                bf.append(area.getText());
                bf.flush();
                bf.close();
            } catch (IOException e) {
                Platform.runLater(() -> SceneHandler.getInstance().showErrorMessage("Error", "Cannot save file"));
            }
        }).start();
    }

    @FXML
    void exitMenu() {
        SceneHandler.getInstance().getPrimaryStage().close();
    }

    @FXML
    void settingsMenu() {
        SceneHandler.getInstance().showSettingsWindow();
    }

    @FXML
    void aboutMenu() {
		String message = """
				Welcome to E-ASP: an explanation tool for ASP.
				Project home: https://github.com/MarcoMochi/E-ASP.
				""";
		SceneHandler.getInstance().showInfoMessage("About", message);
    }

    private Tab createNewTab(String nome) {
        Tab t = new Tab(nome);
        tabPane.getTabs().add(t);
        tabPane.getSelectionModel().select(t);
        InlineCssTextArea area = new InlineCssTextArea();
        VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(area);
        area.setParagraphGraphicFactory(LineNumberFactory.get(area));
        t.setContent(vsPane);
        area.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.S && (ke.isControlDown() || ke.isMetaDown())) {
                save();
            } else if (!t.getText().endsWith("*")) t.setText(t.getText() + "*");
        });
        return t;
    }

    @FXML
    void initialize() {
        menuBar.setUseSystemMenuBar(true);
        tab2file = new HashMap<>();
        newMenuItem.setGraphic(new CustomFontIcon("mdi2f-file"));
        openMenuItem.setGraphic(new CustomFontIcon("mdi2f-folder-open"));
        saveMenuItem.setGraphic(new CustomFontIcon("mdi2c-content-save"));
        exitMenuItem.setGraphic(new CustomFontIcon("mdi2e-exit-to-app"));
        aboutMenuItem.setGraphic(new CustomFontIcon("mdi2i-information"));
        settingsMenuItem.setGraphic(new CustomFontIcon("mdi2a-account"));
        openFileService.setOnSucceeded(e -> {
            Tab t = createNewTab(openFileService.getChosenFile().getName());
            @SuppressWarnings("unchecked") VirtualizedScrollPane<InlineCssTextArea> vs = (VirtualizedScrollPane<InlineCssTextArea>) t.getContent();
            InlineCssTextArea area = vs.getContent();
            area.appendText(e.getSource().getValue().toString());
            tab2file.put(t, openFileService.getChosenFile());
        });
        openFileService.setOnFailed(e -> SceneHandler.getInstance().showErrorMessage("Error", "Cannot open file!"));
        progressBar.visibleProperty().bind(computeFirstAnswerSetService.runningProperty());
        progressBar.progressProperty().bind(computeFirstAnswerSetService.progressProperty());
    }

}
