package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLink;

public interface LtiAdvantageLinkReaderService extends BrightspaceReaderService<LtiAdvantageLink, LtiAdvantageLinkReaderService> {

    Optional<LtiAdvantageLink> get(String orgUnitId, long linkId) throws IOException;

}
