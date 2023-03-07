package edu.iu.terracotta.model.test;

import edu.iu.terracotta.model.app.Condition;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Conditions {

    private List<Condition> conditions = new ArrayList<>();

}
