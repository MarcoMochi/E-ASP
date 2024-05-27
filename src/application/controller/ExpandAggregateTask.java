package application.controller;

import application.model.debugger.Justifier;
import application.model.debugger.QueryAtom;
import javafx.concurrent.Task;

import java.util.List;
import java.util.Map;

public class ExpandAggregateTask extends Task<Map<String,Map<String, List<String>>>> {

    private final Justifier justifier;
    private final String rule;
    private final List<QueryAtom> answerSet;

    public ExpandAggregateTask(Justifier justifier, String rule, List<QueryAtom> answerSet) {
        super();
        this.justifier = justifier;
        this.rule = rule;
        this.answerSet = answerSet;
    }

    protected Map<String,Map<String, List<String>>> call() throws Exception {
        return justifier.expandAggregate(rule, answerSet);
    }
}
