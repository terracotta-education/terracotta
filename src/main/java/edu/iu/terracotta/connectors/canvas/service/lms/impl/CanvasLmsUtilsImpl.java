package edu.iu.terracotta.connectors.canvas.service.lms.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;

@Service
@TerracottaConnector(LmsConnector.CANVAS)
public class CanvasLmsUtilsImpl implements LmsUtils {

    @Override
    public String parseCourseId(PlatformDeployment platformDeployment, String url) throws TerracottaConnectorException {
        return StringUtils.substringBetween(
            url,
            "courses/",
            "/names"
        );
    }

    @Override
    public String parseDeploymentId(PlatformDeployment platformDeployment, String url) throws TerracottaConnectorException {
        throw new UnsupportedOperationException("Unimplemented method 'parseDeploymentId'");
    }

    @Override
    public String sanitize(String input) {
        return input;
    }

}
