package debugger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class DebuggerUtil {
	public static String solver = "clingo";
	public static String helper = "/Users/marco/Documents/GitHub/E-ASP/helper/helper.lp";

	private static String settings = "set_path";

	public static void readSettings() {
		File f = new File(settings);
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
		File f = new File(settings);
		try {
			FileWriter fw = new FileWriter(f, false);
			fw.append(solver);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
