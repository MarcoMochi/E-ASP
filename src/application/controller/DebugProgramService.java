package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import application.model.debugger.Response;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;

public class DebugProgramService extends Service<List<Response>> {

    private Justifier justifier;
    
    public void setParameters(Justifier justifier) {
        this.justifier = justifier;
    }


    @Override
    protected Task<List<Response>> createTask() {
        return new Task<>() {
            @Override
            protected List<Response> call() throws Exception {
                return justifier.debug();
            }
        };
    }
}
