package edu.iu.terracotta.dao.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepDto {

    private String step;
    private Map<String,String> parameters;

}
