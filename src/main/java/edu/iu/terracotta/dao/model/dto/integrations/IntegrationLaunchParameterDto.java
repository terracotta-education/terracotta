package edu.iu.terracotta.dao.model.dto.integrations;

import java.util.UUID;

import edu.iu.terracotta.dao.model.enums.integrations.IntegrationLaunchParameter;
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
public class IntegrationLaunchParameterDto {

    private UUID id;
    private IntegrationLaunchParameter key;
    private boolean enabled;

}
