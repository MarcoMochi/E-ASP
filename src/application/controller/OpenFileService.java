package application.controller;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenFileService extends Service<String> {

    private File chosenFile;

    void setChosenFile(File chosenFile) {
        this.chosenFile = chosenFile;
    }

    File getChosenFile() {
        return chosenFile;
    }

    @Override
    protected Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                long fileSize = Files.size(Path.of(chosenFile.toURI()));
                long mb = fileSize/(1024*1024);
                if(mb > 10)
                    throw new Exception("Cannot open files larger than 10 MB");
                return Files.readString(Path.of(chosenFile.toURI()));
            }
        };
    }
}
