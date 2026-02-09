package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageQuickLinkReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageQuickLinkWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageQuickLink;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

public class LtiAdvantageQuickLinkServiceImpl extends BaseServiceImpl<LtiAdvantageQuickLink, LtiAdvantageQuickLinkReaderService, LtiAdvantageQuickLinkWriterService> implements LtiAdvantageQuickLinkReaderService, LtiAdvantageQuickLinkWriterService {

    public LtiAdvantageQuickLinkServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public Optional<LtiAdvantageQuickLink> create(String orgUnitId, long ltiAdvantageLinkId) throws IOException {
        Response response = brightspaceMessenger.sendJsonPost(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.LTI_ADVANTAGE_QUICKLINK_ROOT.url(),
                    apiVersion.getLe(),
                    orgUnitId,
                    ltiAdvantageLinkId
                )
            ),
            null
        );

        return Optional.of(
            responseParser.parseToObject(LtiAdvantageQuickLink.class, response)
                .orElseThrow(() -> new IOException("Error creating LTI Advantage Quick Link"))
        );
    }

    @Override
    protected TypeReference<List<LtiAdvantageQuickLink>> listType() {
        return new TypeReference<List<LtiAdvantageQuickLink>>() {};
    }

    @Override
    protected Class<LtiAdvantageQuickLink> objectType() {
        return LtiAdvantageQuickLink.class;
    }

}
