package edu.iu.terracotta.model.app.integrations.dto;

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
public class IntegrationConfigurationDto {

    private UUID id;
    private UUID integrationId;
    private IntegrationClientDto client;
    private String launchUrl;
    private boolean feedbackEnabled;

}
