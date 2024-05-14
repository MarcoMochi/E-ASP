package debugger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class DebuggerUtil {
	public static String solver = "/opt/homebrew/bin/clingo";
	public static String helper = "./helper/helper.lp";

	public static String paths = "./set_path";

	public static void readSettings() {
		File f = new File(paths);
		if (!f.exists())
			writeSettings();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			while (br.ready()) {
				String s = br.readLine();
				if (s.contains("CLINGO_PATH"))
					solver = s.split("=")[1].trim();
				else if (s.contains("HELPER_PATH"))
					helper = s.split("=")[1].trim();
				else
					break;
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeSettings() {
		File f = new File(paths);
		try {
			FileWriter fw = new FileWriter(f, false);
			fw.append(solver);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
