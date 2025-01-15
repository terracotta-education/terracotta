package edu.iu.terracotta.connectors.generic.service.connector;

import java.util.Optional;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

public interface ConnectorService<T> {

    T instance(long platformDeploymentId, Class<?> type) throws TerracottaConnectorException;
    T instance(Optional<PlatformDeployment> platformDeployment, Class<?> type) throws TerracottaConnectorException;
    T instance(PlatformDeployment platformDeployment, Class<?> type) throws TerracottaConnectorException;

}
