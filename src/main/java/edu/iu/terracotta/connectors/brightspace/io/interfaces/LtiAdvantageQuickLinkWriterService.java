package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageQuickLink;

public interface LtiAdvantageQuickLinkWriterService extends BrightspaceWriterService<LtiAdvantageQuickLink, LtiAdvantageQuickLinkWriterService> {

    Optional<LtiAdvantageQuickLink> create(String orgUnitId, long ltiAdvantageLinkId) throws IOException;

}
