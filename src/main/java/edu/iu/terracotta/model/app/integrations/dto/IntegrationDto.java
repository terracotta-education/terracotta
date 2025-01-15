package edu.iu.terracotta.model.app.integrations.dto;

import java.util.List;
import java.util.UUID;

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
public class IntegrationDto {

    private UUID id;
    private long questionId;
    private IntegrationConfigurationDto configuration;
    private List<IntegrationClientDto> clients;
    private String previewUrl;

}
