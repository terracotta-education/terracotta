package edu.iu.terracotta.connectors.oneedtech.service.lms.impl;

import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.annotation.TerracottaConnector;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.PlatformDeployment;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUtils;

@Service
@TerracottaConnector(LmsConnector.ONE_ED_TECH)
public class OneEdTechLmsUtilsImpl implements LmsUtils {

    @Override
    public String parseCourseId(PlatformDeployment platformDeployment, String url) {
        return url;
    }

}
