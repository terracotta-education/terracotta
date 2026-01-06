package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import java.util.List;
import java.util.Map;

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
@SuppressWarnings({"PMD.LooseCoupling"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolConfigurationDto {

    private String domain;
    private List<String> secondary_domains;
    private String deployment_id;
    private String target_link_uri;
    private Map<String, String> custom_parameters;
    private String description;
    private List<ToolMessagesSupportedDto> messages_supported;
    private List<String> claims;

}
