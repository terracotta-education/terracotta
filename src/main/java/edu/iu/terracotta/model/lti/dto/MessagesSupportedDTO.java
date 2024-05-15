package edu.iu.terracotta.model.lti.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessagesSupportedDTO {

    private String type;
    private List<String> placements;

}
