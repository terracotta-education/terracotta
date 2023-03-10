package edu.iu.terracotta.model.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutcomePotentialDto {

    public String name;
    public String type;
    public Integer assignmentId;
    public Double pointsPossible;
    public boolean terracotta;

}
