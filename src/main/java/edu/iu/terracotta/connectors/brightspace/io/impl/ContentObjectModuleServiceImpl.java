package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectModuleReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ContentObjectModuleWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModule;
import edu.iu.terracotta.connectors.brightspace.io.model.ContentObjectModuleUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

public class ContentObjectModuleServiceImpl extends BaseServiceImpl<ContentObjectModule, ContentObjectModuleReaderService, ContentObjectModuleWriterService> implements ContentObjectModuleReaderService, ContentObjectModuleWriterService {

    public ContentObjectModuleServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public Optional<ContentObjectModule> create(String orgUnitId, ContentObjectModuleUpdate contentObjectModuleUpdate) throws IOException {
        Response response = brightspaceMessenger.sendJsonPost(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.CONTENT_MODULE_ROOT.url(),
                    apiVersion.getLe(),
                    orgUnitId
                )
            ),
            contentObjectModuleUpdate.toJson(serializeNulls)
        );

        return Optional.of(
            responseParser.parseToObject(ContentObjectModule.class, response)
                .orElseThrow(() -> new IOException("Error creating Content Object Module"))
        );
    }

    @Override
    public List<ContentObjectModule> getAllForOrgUnitId(String orgUnitId) throws IOException {
        return parseList(
            getListResponse(
                buildUrl(
                    String.format(
                        BrightspaceUrl.CONTENT_MODULE_ROOT.url(),
                        apiVersion.getLe(),
                        orgUnitId
                    )
                )
            )
        );
    }

    @Override
    public Optional<ContentObjectModule> get(String orgUnitId, long contentObjectModuleId) throws IOException {
        return responseParser.parseToObject(
            ContentObjectModule.class,
            brightspaceMessenger.getSingleResponse(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.CONTENT_MODULE.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        contentObjectModuleId
                    )
                )
            )
        );
    }

    @Override
    public Optional<ContentObjectModule> update(String orgUnitId, long contentObjectModuleId, ContentObjectModuleUpdate contentObjectModuleUpdate) throws IOException {
        return responseParser.parseToObject(
            ContentObjectModule.class,
            brightspaceMessenger.sendJsonPut(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.CONTENT_MODULE.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        contentObjectModuleId
                    )
                ),
                contentObjectModuleUpdate.toJson(serializeNulls)
            )
        );
    }

    @Override
    public void delete(String orgUnitId, long contentObjectModuleId) throws IOException {
        brightspaceMessenger.delete(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.CONTENT_MODULE.url(),
                    apiVersion.getLe(),
                    orgUnitId,
                    contentObjectModuleId
                )
            )
        );
    }

    @Override
    protected TypeReference<List<ContentObjectModule>> listType() {
        return new TypeReference<List<ContentObjectModule>>() {};
    }

    @Override
    protected Class<ContentObjectModule> objectType() {
        return ContentObjectModule.class;
    }

    private List<Response> getListResponse(String url) throws IOException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        List<Response> responses = brightspaceMessenger.get(oauthToken, url, consumer);
        responseCallback = null;

        return responses;
    }

    private List<ContentObjectModule> parseList(List<Response> responses) {
        List<ContentObjectModule> contentObjectModuleList = new ArrayList<>();

        responses.stream()
            .forEach(response -> contentObjectModuleList.addAll(parseResponseList(response)));

        return contentObjectModuleList;
    }

    private List<ContentObjectModule> parseResponseList(Response response) {
        return responseParser.parseToList(
            listType(),
            response
        );
    }

}
