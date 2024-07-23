package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;

public class ComputeFirstAnswerSetService extends Service<List<QueryAtom>> {

    private Justifier justifier;

    public void setJustifier(Justifier justifier) {
        this.justifier = justifier;
    }

    @Override
    protected Task<List<QueryAtom>> createTask() {
        return new Task<>() {
            @Override
            protected List<QueryAtom> call() throws Exception {
                return justifier.computeFirstAnswerSet();
            }
        };
    }
}
