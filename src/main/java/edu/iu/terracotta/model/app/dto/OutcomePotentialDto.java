package edu.iu.terracotta.model.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutcomePotentialDto {

    private String name;
    private String type;
    private Integer assignmentId;
    private Double pointsPossible;
    private boolean terracotta;

}
