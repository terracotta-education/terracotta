package edu.iu.terracotta.dao.model.distribute.export;

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
public class IntegrationClientExport {

    private long id;
    private String name;
    private String scoreVariable;
    private String tokenVariable;
    private String previewToken;
    private boolean enabled;

}
