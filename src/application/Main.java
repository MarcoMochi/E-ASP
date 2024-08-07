package application;
	
import application.view.SceneHandler;
import javafx.application.Application;
import javafx.stage.Stage;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		Settings.readSettings();
		SceneHandler.getInstance().init(primaryStage);
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
