package edu.iu.terracotta.model.test;

import edu.iu.terracotta.model.app.Condition;

import java.util.ArrayList;
import java.util.List;

public class Conditions {

    private List<Condition> conditions = new ArrayList<>();

    public Conditions() {}

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }
}
