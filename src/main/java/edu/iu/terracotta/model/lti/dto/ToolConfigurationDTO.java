package edu.iu.terracotta.model.lti.dto;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToolConfigurationDTO {

    private String domain;
    private List<String> secondary_domains;
    private String deployment_id;
    private String target_link_uri;
    private Map<String, String> custom_parameters;
    private String description;
    private List<ToolMessagesSupportedDTO> messages_supported;
    private List<String> claims;

}
