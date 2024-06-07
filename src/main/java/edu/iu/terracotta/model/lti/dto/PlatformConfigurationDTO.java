package edu.iu.terracotta.model.lti.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformConfigurationDTO {

    private String product_family_code;
    private String version;
    private List<MessagesSupportedDTO> messages_supported;
    private List<String> variables;

}
