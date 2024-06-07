package edu.iu.terracotta.model.lti.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToolMessagesSupportedDTO {

    private String type;
    private String target_link_uri;
    private String label;
    private String icon_uri;
    private Map<String, String> custom_parameters;
    private List<String> placements;

}
