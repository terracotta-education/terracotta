package edu.iu.terracotta.dao.repository.integrations;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.dao.entity.integrations.IntegrationConfiguration;


@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface IntegrationConfigurationRepository extends JpaRepository<IntegrationConfiguration, Long> {

}
