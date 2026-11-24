package edu.iu.terracotta.dao.model.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutcomePotentialDto {

    public String name;
    public String type;
    public String assignmentId;
    public Float pointsPossible;
    public boolean terracotta;
    // shown when the instructor opens a gradebook item to check it's the right one
    public String htmlUrl;
    public Date dueAt;
    public String description;

}
