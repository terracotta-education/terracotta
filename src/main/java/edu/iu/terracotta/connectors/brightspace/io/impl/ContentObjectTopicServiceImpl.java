package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectTopicReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectTopicWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopic;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectTopicUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

public class ContentObjectTopicServiceImpl extends BaseServiceImpl<ContentObjectTopic, ContentObjectTopicReaderService, ContentObjectTopicWriterService> implements ContentObjectTopicReaderService, ContentObjectTopicWriterService {

    public ContentObjectTopicServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public Optional<ContentObjectTopic> create(String orgUnitId, long contentObjectModuleId, ContentObjectTopicUpdate contentObjectTopicUpdate) throws IOException {
        Response response = brightspaceMessenger.sendJsonPost(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.CONTENT_MODULE_STRUCTURE.url(),
                    apiVersion.getLe(),
                    orgUnitId,
                    contentObjectModuleId
                )
            ),
            contentObjectTopicUpdate.toJson(serializeNulls)
        );

        return Optional.of(
            responseParser.parseToObject(ContentObjectTopic.class, response)
                .orElseThrow(() -> new IOException("Error creating Content Object Topic"))
        );
    }

    @Override
    public Optional<ContentObjectTopic> get(String orgUnitId, long contentObjectTopicId) throws IOException {
        return responseParser.parseToObject(
            ContentObjectTopic.class,
            brightspaceMessenger.getSingleResponse(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.CONTENT_TOPIC.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        contentObjectTopicId
                    )
                )
            )
        );
    }

    @Override
    public Optional<ContentObjectTopic> update(String orgUnitId, long contentObjectTopicId, ContentObjectTopicUpdate contentObjectTopicUpdate) throws IOException {
        return responseParser.parseToObject(
            ContentObjectTopic.class,
            brightspaceMessenger.sendJsonPut(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.CONTENT_TOPIC.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        contentObjectTopicId
                    )
                ),
                contentObjectTopicUpdate.toJson(serializeNulls)
            )
        );
    }

    @Override
    public void delete(String orgUnitId, long contentObjectTopicId) throws IOException {
        brightspaceMessenger.delete(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.CONTENT_TOPIC.url(),
                    apiVersion.getLe(),
                    orgUnitId,
                    contentObjectTopicId
                )
            )
        );
    }

    @Override
    protected TypeReference<List<ContentObjectTopic>> listType() {
        return new TypeReference<List<ContentObjectTopic>>() {};
    }

    @Override
    protected Class<ContentObjectTopic> objectType() {
        return ContentObjectTopic.class;
    }

}
