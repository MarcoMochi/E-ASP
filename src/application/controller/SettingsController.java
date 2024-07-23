package application.controller;

import application.Settings;
import application.view.FileMenu;
import application.view.SceneHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;

import java.io.File;

public class SettingsController {

    @FXML
    private Label label;

    private Window window = null;

    @FXML
    private ComboBox<String> themeComboBox;

    public void setWindow(Window window) {
        this.window = window;
    }

    @FXML
    void changePath() {
        File chosenFile = FileMenu.openFileChooser(window);
        if (chosenFile != null) {
            Settings.changeSolverPath(chosenFile.getAbsolutePath());
            setCurrentSolver();
        }
    }

    @FXML
    void changeTheme() {
        SceneHandler.getInstance().setTheme(themeComboBox.getSelectionModel().getSelectedItem());
        selectCurrentTheme();
    }

    private void selectCurrentTheme() {
        if("dark".equals(Settings.getTheme()))
            themeComboBox.getSelectionModel().select("Dark");
        else
            themeComboBox.getSelectionModel().select("Light");
    }

    private void setCurrentSolver() {
        label.setText("Solver's path: " + Settings.getSolverPath());
        label.setTooltip(new Tooltip(Settings.getSolverPath()));
    }

    @FXML
    void initialize() {
        themeComboBox.getItems().add("Dark");
        themeComboBox.getItems().add("Light");
        selectCurrentTheme();
        setCurrentSolver();
    }

}
