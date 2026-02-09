package edu.iu.terracotta.connectors.brightspace.service.lms.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;

@Service
@TerracottaConnector(LmsConnector.BRIGHTSPACE)
public class BrightspaceLmsUtilsImpl implements LmsUtils {

    @Override
    public String parseCourseId(PlatformDeployment platformDeployment, String url) {
        return StringUtils.substringBetween(
            url,
            "orgunit/",
            "/memberships"
        );
    }

    @Override
    public String parseDeploymentId(PlatformDeployment platformDeployment, String url) {
        return StringUtils.substringBetween(
            url,
            "deployment/",
            "/orgunit"
        );
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
