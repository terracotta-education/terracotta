package edu.iu.terracotta.repository.integrations;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.iu.terracotta.model.app.integrations.IntegrationConfiguration;

@SuppressWarnings({"PMD.MethodNamingConventions"})
public interface IntegrationConfigurationRepository extends JpaRepository<IntegrationConfiguration, Long> {

}
