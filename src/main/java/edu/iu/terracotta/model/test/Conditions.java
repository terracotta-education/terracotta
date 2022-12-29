package edu.iu.terracotta.model.test;

import edu.iu.terracotta.model.app.Condition;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Conditions {

    private List<Condition> conditionList = new ArrayList<>();

}
