package edu.iu.terracotta.dao.model.dto.integrations;

import java.util.List;
import java.util.UUID;

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
public class IntegrationDto {

    private UUID id;
    private long questionId;
    private IntegrationConfigurationDto configuration;
    private List<IntegrationClientDto> clients;
    private String previewUrl;

}
