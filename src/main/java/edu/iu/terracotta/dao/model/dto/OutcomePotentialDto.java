package edu.iu.terracotta.dao.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutcomePotentialDto {

    public String name;
    public String type;
    public String assignmentId;
    public Float pointsPossible;
    public boolean terracotta;

}
