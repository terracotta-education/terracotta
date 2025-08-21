package edu.iu.terracotta.connectors.generic.dao.model.lti.dto;

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
public class DeepLinkJwtDto {

    private String jwt;
    private String returnUrl;

}
