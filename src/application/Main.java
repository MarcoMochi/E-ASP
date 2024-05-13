package application;
	
import debugger.DebuggerUtil;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("Editor.fxml"));
			AnchorPane root = (AnchorPane) loader.load();
			EditorController controller = loader.getController();
			Scene scene = new Scene(root,800,800);
			controller.init(primaryStage);
			primaryStage.setScene(scene);
			DebuggerUtil.readSettings();
			primaryStage.show();			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
