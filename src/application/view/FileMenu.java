package application.view;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

public class FileMenu {

    public static File openFileChooser(Window parent) {
        return handleFileChooser(parent, true);
    }

    public static File saveFileChooser(Window parent) {
        return handleFileChooser(parent, false);
    }

    private static File handleFileChooser(Window parent, boolean open) {
        FileChooser f = new FileChooser();
        File chosenFile = open ? f.showOpenDialog(parent) : f.showSaveDialog(parent);
        if(chosenFile == null || chosenFile.isDirectory())
            return null;
        return chosenFile;
    }
}
