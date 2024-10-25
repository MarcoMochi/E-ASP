package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import application.model.debugger.Response;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;

public class ExplainAtomService extends Service<List<Response>> {

    private Justifier justifier;
    private List<QueryAtom> chain;
    private QueryAtom atom;
    private Boolean checkOpt;

    public void setParameters(Justifier justifier, List<QueryAtom> chain, QueryAtom atom, Boolean checkOpt) {
        this.justifier = justifier;
        this.chain = chain;
        this.atom = atom;
        this.checkOpt = checkOpt;
    }


    @Override
    protected Task<List<Response>> createTask() {
        return new Task<>() {
            @Override
            protected List<Response> call() throws Exception {
                return justifier.justify(chain, atom, checkOpt);
            }
        };
    }
}
