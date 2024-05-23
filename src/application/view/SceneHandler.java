package application.view;

import application.Settings;
import application.controller.AnswerSetInspectionController;
import application.controller.ExplanationController;
import application.controller.SettingsController;
import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import application.model.debugger.Response;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;

public class SceneHandler {

    private record LoaderRootObject<T extends Parent>(FXMLLoader loader, T root) {}

    private static final String RESOURCES_PATH = "/application/resources/";
    private static final String CSS_PATH = RESOURCES_PATH + "css/";
    private static final String FONTS_PATH = RESOURCES_PATH + "fonts/";

    private String theme;

    private static final SceneHandler instance = new SceneHandler();
    private final Alert alert;
    private Scene scene;
    private Stage primaryStage;

    private Parent homeRoot;
    private final Stack<Parent> explanationsStack = new Stack<>();

    private SceneHandler() {
        alert = new Alert(Alert.AlertType.NONE);
    }

    public static SceneHandler getInstance() {
        return instance;
    }

    public void init(Stage stage) {
        if(scene != null)
            return;
        primaryStage = stage;
        theme = Settings.getTheme();
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(400);
        homeRoot = loadFXML("Editor.fxml").root();
        scene = new Scene(homeRoot,800,800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("E-ASP");
        for (String font : List.of(FONTS_PATH + "Roboto/Roboto-Regular.ttf", FONTS_PATH + "Roboto/Roboto-Bold.ttf"))
            Font.loadFont(Objects.requireNonNull(SceneHandler.class.getResource(font)).toExternalForm(), 10);

        loadStyle();
        primaryStage.show();
    }

    private <T extends Parent> LoaderRootObject<T> loadFXML(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(RESOURCES_PATH + fxmlFile));
            return new LoaderRootObject<>(loader, loader.load());
        } catch(Exception e) {
            SceneHandler.getInstance().showErrorMessage("Error", "Error 101. Application cannot be executed.");
            System.exit(101);
        }
        return null;
    }

    public void backHome() {
        if(scene != null && homeRoot != null) {
            scene.setRoot(homeRoot);
            explanationsStack.clear();
        }
    }

    public void showSettingsWindow() {
        Stage modalStage = new Stage();
        modalStage.setTitle("Settings");
        modalStage.setWidth(400);
        modalStage.setHeight(150);
        modalStage.setResizable(false);
        modalStage.initModality(Modality.APPLICATION_MODAL);
        LoaderRootObject<Parent> obj = loadFXML("Settings.fxml");
        Scene currentScene = new Scene(obj.root());
        SettingsController controller = obj.loader().getController();
        controller.setWindow(modalStage);
        modalStage.setScene(currentScene);
        loadStyle(currentScene.getStylesheets());
        modalStage.show();
    }

    public void showInspectAnswerSetWindow(List<QueryAtom> answerSet, Justifier justifier) {
        LoaderRootObject<Parent> obj = loadFXML("AnswerSetInspection.fxml");
        explanationsStack.add(obj.root());
        scene.setRoot(obj.root());
        AnswerSetInspectionController controller = obj.loader().getController();
        controller.init(answerSet, justifier);
    }

    public void showExplanationWindow(Justifier justifier, List<QueryAtom> answerSet, List<QueryAtom> chain, List<Response> response) {
        LoaderRootObject<Parent> obj = loadFXML("Explanation.fxml");
        explanationsStack.add(obj.root());
        scene.setRoot(obj.root());
        ExplanationController controller = obj.loader().getController();
        controller.init(justifier, answerSet, chain, response);
    }

    public void back() {
        if(explanationsStack.empty())
            return;
        explanationsStack.pop();
        if(!explanationsStack.empty())
            scene.setRoot(explanationsStack.peek());
    }

    private void loadStyle() {
        loadStyle(scene.getStylesheets());
        loadStyle(alert.getDialogPane().getStylesheets());
    }

    private void loadStyle(ObservableList<String> styleSheets) {
        styleSheets.clear();
        for (String style : List.of(CSS_PATH + theme + ".css", CSS_PATH + "fonts.css", CSS_PATH + "style.css")) {
            String resource = Objects.requireNonNull(SceneHandler.class.getResource(style)).toExternalForm();
            styleSheets.add(resource);
        }
    }

    public void showErrorMessage(String title, String text) {
        showAlert(title, text, Alert.AlertType.ERROR);
    }

    public void showInfoMessage(String title, String text) {
        showAlert(title, text, Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String text, Alert.AlertType type) {
        alert.setAlertType(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    public void setTheme(String selectedItem) {
        theme = selectedItem.toLowerCase(Locale.ROOT);
        Settings.updateTheme(theme);
        loadStyle();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
