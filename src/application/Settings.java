package application;

import application.view.SceneHandler;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class Settings {
	private static String solverPath = "clingo";
	private static String helperPath = "helper" + File.separator + "helper.lp";
	private static String theme = "dark";
	public static final String paths = "config.json";

	public static void readSettings() {
		File f = new File(paths);
		if (!f.exists())
			writeSettings();
		try {
			String content = Files.readString(Path.of(paths));
			JSONObject object = new JSONObject(content);
			solverPath = object.getString("solver");
			helperPath = object.getString("helper");
			theme = object.getString("theme").toLowerCase(Locale.ROOT);
			if(!"dark".equals(theme) && !"light".equals(theme))
				throw new Exception("Invalid theme");
		} catch (Exception e) {
			SceneHandler.getInstance().showErrorMessage("Error", "Cannot load settings.");
			System.exit(1);
		}
	}

	private static void writeSettings() {
		try {
			JSONObject object = new JSONObject();
			object.put("solver", solverPath);
			object.put("helper", helperPath);
			object.put("theme", theme);
			Files.writeString(Path.of(paths), object.toString(2));
		} catch (IOException e) {
			SceneHandler.getInstance().showErrorMessage("Error", "Cannot save settings.");
		}
	}

	public static void changeSolverPath(String path) {
		solverPath = path;
		writeSettings();
	}

	public static void updateTheme(String theme) {
		Settings.theme = theme;
		writeSettings();
	}

	public static String getSolverPath() {
		return solverPath;
	}

	public static String getHelperPath() {
		return helperPath;
	}

	public static String getTheme() {
		return theme;
	}
}
