package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessagesSupportedDto {

    private String type;
    private List<String> placements;

}
