package edu.iu.terracotta.connectors.generic.service.lms;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;

@TerracottaConnector(LmsConnector.GENERIC)
public interface LmsUtils {

    String parseCourseId(PlatformDeployment platformDeployment, String url) throws TerracottaConnectorException;
    String parseDeploymentId(PlatformDeployment platformDeployment, String url) throws TerracottaConnectorException;
    String sanitize(String input);

}
