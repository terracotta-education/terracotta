package edu.iu.terracotta.dao.model.dto.integrations;

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

}
