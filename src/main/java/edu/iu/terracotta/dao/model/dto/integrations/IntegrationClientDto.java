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
public class IntegrationClientDto {

    private UUID id;
    private String name;
    private String returnUrl;
    private String previewToken;

}
