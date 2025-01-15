package edu.iu.terracotta.dao.model.dto.integrations;

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
public class IntegrationUrlValidationDto {

    private String url;
    private boolean valid;
    private String error;

}
