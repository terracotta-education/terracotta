package edu.iu.terracotta.connectors.generic.service.lms.impl;

import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.connector.ConnectorService;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;

@Primary
@Service
public class LmsUtilsImpl implements LmsUtils {

    @Autowired private ConnectorService<LmsUtils> connectorService;

    private LmsUtils instance(PlatformDeployment platformDeployment) throws TerracottaConnectorException {
        return connectorService.instance(platformDeployment, LmsUtils.class);
    }

    @Override
    public String parseCourseId(PlatformDeployment platformDeployment, String url) throws TerracottaConnectorException {
        return instance(platformDeployment).parseCourseId(platformDeployment, url);
    }

    @Override
    public String parseDeploymentId(PlatformDeployment platformDeployment, String url) throws TerracottaConnectorException {
        return instance(platformDeployment).parseDeploymentId(platformDeployment, url);
    }

    @Override
    public String sanitize(String input) {
        input = Strings.CS.replace(input, "/", "_");
        input = Strings.CS.replace(input, "\\", "_");
        input = Strings.CS.replace(input, "\"", "_");
        input = Strings.CS.replace(input, "*", "_");
        input = Strings.CS.replace(input, "<", "_");
        input = Strings.CS.replace(input, ">", "_");
        input = Strings.CS.replace(input, "+", "_");
        input = Strings.CS.replace(input, "=", "_");
        input = Strings.CS.replace(input, "|", "_");
        input = Strings.CS.replace(input, ",", "_");
        input = Strings.CS.replace(input, "%", "_");
        input = Strings.CS.replace(input, ":", "_");
        input = Strings.CS.replace(input, "?", "_");
        input = Strings.CS.replace(input, "~", "_");
        input = Strings.CS.replace(input, "#", "_");
        input = Strings.CS.replace(input, "&", "_");
        input = Strings.CS.replace(input, "{", "_");
        input = Strings.CS.replace(input, "}", "_");

        return input;
    }

}
