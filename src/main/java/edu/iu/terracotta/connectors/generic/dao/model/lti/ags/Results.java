package edu.iu.terracotta.connectors.generic.dao.model.lti.ags;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Results {

    private List<Result> resultList = new ArrayList<>();

}
