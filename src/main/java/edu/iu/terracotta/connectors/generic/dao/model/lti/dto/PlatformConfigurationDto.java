package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

import java.util.List;

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
public class PlatformConfigurationDto {

    private String product_family_code;
    private String version;
    private List<MessagesSupportedDto> messages_supported;
    private List<String> variables;

}
