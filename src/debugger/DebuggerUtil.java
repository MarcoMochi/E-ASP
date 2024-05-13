package debugger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class DebuggerUtil {
	public static String solver = "/Users/marco/opt/miniconda3/envs/potassco/bin/clingo";
	public static String solver2 = "/Users/marco/opt/miniconda3/envs/potassco/bin/clingo";
	public static String helper = "/Users/marco/Documents/Dottorato/Lavori/Debugger/Encoders/helper.lp";

	private static String settings = ".mysettings";

	public static void readSettings() {
		File f = new File(settings);
		if (!f.exists())
			writeSettings();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			while (br.ready()) {
				String s = br.readLine();
				solver = s;
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
