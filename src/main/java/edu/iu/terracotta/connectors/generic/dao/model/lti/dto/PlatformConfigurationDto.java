package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformConfigurationDto {

    private String product_family_code;
    private String version;
    private List<MessagesSupportedDto> messages_supported;
    private List<String> variables;

}
