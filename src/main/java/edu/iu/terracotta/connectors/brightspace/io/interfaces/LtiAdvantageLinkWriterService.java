package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLink;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLinkUpdate;

public interface LtiAdvantageLinkWriterService extends BrightspaceWriterService<LtiAdvantageLink, LtiAdvantageLinkWriterService> {

    Optional<LtiAdvantageLink> create(String orgUnitId, LtiAdvantageLinkUpdate ltiAdvantageLinkUpdate) throws IOException;
    Optional<LtiAdvantageLink> update(String orgUnitId, long linkId, LtiAdvantageLinkUpdate ltiAdvantageLinkUpdate) throws IOException;
    void delete(String orgUnitId, long linkId) throws IOException;

}
