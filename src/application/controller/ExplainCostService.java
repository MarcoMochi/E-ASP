package application.controller;

import application.model.debugger.CostLevel;
import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import application.model.debugger.Response;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;

public class ExplainCostService extends Service<List<Response>> {

    private Justifier justifier;
    private CostLevel level;
    private Boolean checkOpt;

    public void setParameters(Justifier justifier, CostLevel level, Boolean checkOpt) {
        this.justifier = justifier;
        this.level = level;
        this.checkOpt = checkOpt;
    }


    @Override
    protected Task<List<Response>> createTask() {
        return new Task<>() {
            @Override
            protected List<Response> call() throws Exception {
                return justifier.justifyCost(level, checkOpt);
            }
        };
    }
}
