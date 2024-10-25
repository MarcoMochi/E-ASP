package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;

public class ComputeMultiAnswerSetService extends Service<List<String>> {

    private Justifier justifier;
    private Integer n;
    
    public void setJustifier(Justifier justifier, Integer n) {
        this.justifier = justifier;
        this.n = n;
    }

    @Override
    protected Task<List<String>> createTask() {
        return new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return justifier.computeAnswerSets(n);
            }
        };
    }
}
