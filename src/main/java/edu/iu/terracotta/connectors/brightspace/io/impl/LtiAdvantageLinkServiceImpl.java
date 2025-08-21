package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageLinkReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.LtiAdvantageLinkWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLink;
import edu.iu.terracotta.connectors.brightspace.io.model.LtiAdvantageLinkUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

public class LtiAdvantageLinkServiceImpl extends BaseServiceImpl<LtiAdvantageLink, LtiAdvantageLinkReaderService, LtiAdvantageLinkWriterService> implements LtiAdvantageLinkReaderService, LtiAdvantageLinkWriterService {

    public LtiAdvantageLinkServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public Optional<LtiAdvantageLink> create(String orgUnitId, LtiAdvantageLinkUpdate ltiAdvantageLinkUpdate) throws IOException {
        Response response = brightspaceMessenger.sendJsonPost(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.LTI_ADVANTAGE_LINK_ROOT.url(),
                    apiVersion.getLe(),
                    orgUnitId
                )
            ),
            ltiAdvantageLinkUpdate.toJson(serializeNulls)
        );

        return Optional.of(
            responseParser.parseToObject(LtiAdvantageLink.class, response)
                .orElseThrow(() -> new IOException("Error creating LTI Advantage Link"))
        );
    }

    @Override
    public Optional<LtiAdvantageLink> get(String orgUnitId, long linkId) throws IOException {
        return responseParser.parseToObject(
            LtiAdvantageLink.class,
            brightspaceMessenger.getSingleResponse(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.LTI_ADVANTAGE_LINK.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        linkId
                    )
                )
            )
        );
    }

    @Override
    public Optional<LtiAdvantageLink> update(String orgUnitId, long linkId, LtiAdvantageLinkUpdate ltiAdvantageLinkUpdate) throws IOException {
        return responseParser.parseToObject(
            LtiAdvantageLink.class,
            brightspaceMessenger.sendJsonPut(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.LTI_ADVANTAGE_LINK.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        linkId
                    )
                ),
                ltiAdvantageLinkUpdate.toJson(serializeNulls)
            )
        );
    }

    @Override
    public void delete(String orgUnitId, long linkId) throws IOException {
        brightspaceMessenger.delete(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.LTI_ADVANTAGE_LINK.url(),
                    apiVersion.getLe(),
                    orgUnitId,
                    linkId
                )
            )
        );
    }

    @Override
    protected TypeReference<List<LtiAdvantageLink>> listType() {
        return new TypeReference<List<LtiAdvantageLink>>() {};
    }

    @Override
    protected Class<LtiAdvantageLink> objectType() {
        return LtiAdvantageLink.class;
    }

}
