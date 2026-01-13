package edu.iu.terracotta.dao.entity.integrations;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_integrations_configuration")
public class IntegrationConfiguration extends BaseUuidEntity {

    @OneToOne(mappedBy = "configuration")
    private Integration integration;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private IntegrationClient client;

    @Column private String launchUrl;

}
