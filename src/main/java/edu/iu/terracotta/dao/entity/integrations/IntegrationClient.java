package edu.iu.terracotta.dao.entity.integrations;

import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_integrations_client")
public class IntegrationClient extends BaseUuidEntity {

    public static final String RETURN_URL = "%s/integrations?launch_token=%s&score=%s";

    @OneToMany(mappedBy = "client")
    private List<IntegrationConfiguration> configuration;

    @Column private String name;
    @Column private String scoreVariable;
    @Column private String tokenVariable;
    @Column private String previewToken;
    @Column private boolean enabled;

}
