package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.DropboxFolderReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.DropboxFolderWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolder;
import edu.iu.terracotta.connectors.brightspace.io.model.DropboxFolderUpdate;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import tools.jackson.core.type.TypeReference;

public class DropboxFolderServiceImpl extends BaseServiceImpl<DropboxFolder, DropboxFolderReaderService, DropboxFolderWriterService> implements DropboxFolderReaderService, DropboxFolderWriterService {

    public DropboxFolderServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public Optional<DropboxFolder> create(String orgUnitId, DropboxFolderUpdate dropboxFolderUpdate) throws IOException {
        Response response = brightspaceMessenger.sendJsonPost(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.DROPBOX_FOLDERS_ROOT.url(),
                    apiVersion.getLe(),
                    orgUnitId
                )
            ),
            dropboxFolderUpdate.toJson(serializeNulls)
        );

        return Optional.of(
            responseParser.parseToObject(DropboxFolder.class, response)
                .orElseThrow(() -> new IOException("Error creating DropBox Folder"))
        );
    }

    @Override
    public List<DropboxFolder> getAllForOrgUnitId(String orgUnitId) throws IOException {
        return parseList(
            getListResponse(
                buildUrl(
                    String.format(
                        BrightspaceUrl.DROPBOX_FOLDERS_ROOT.url(),
                        apiVersion.getLe(),
                        orgUnitId
                    )
                )
            )
        );
    }

    @Override
    public Optional<DropboxFolder> get(String orgUnitId, long dropboxFolderId) throws IOException {
        return responseParser.parseToObject(
            DropboxFolder.class,
            brightspaceMessenger.getSingleResponse(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.DROPBOX_FOLDER.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        dropboxFolderId
                    )
                )
            )
        );
    }

    @Override
    public Optional<DropboxFolder> update(String orgUnitId, long dropboxFolderId, DropboxFolderUpdate dropboxFolderUpdate) throws IOException {
        return responseParser.parseToObject(
            DropboxFolder.class,
            brightspaceMessenger.sendJsonPut(
                oauthToken,
                buildUrl(
                    String.format(
                        BrightspaceUrl.DROPBOX_FOLDER.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        dropboxFolderId
                    )
                ),
                dropboxFolderUpdate.toJson(serializeNulls)
            )
        );
    }

    @Override
    public void delete(String orgUnitId, long dropboxFolderId) throws IOException {
        brightspaceMessenger.delete(
            oauthToken,
            buildUrl(
                String.format(
                    BrightspaceUrl.DROPBOX_FOLDER.url(),
                    apiVersion.getLe(),
                    orgUnitId,
                    dropboxFolderId
                )
            )
        );
    }

    @Override
    protected TypeReference<List<DropboxFolder>> listType() {
        return new TypeReference<List<DropboxFolder>>() {};
    }

    @Override
    protected Class<DropboxFolder> objectType() {
        return DropboxFolder.class;
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

    private List<DropboxFolder> parseList(List<Response> responses) {
        List<DropboxFolder> dropboxFolderList = new ArrayList<>();

        responses.stream()
            .forEach(response -> dropboxFolderList.addAll(parseResponseList(response)));

        return dropboxFolderList;
    }

    private List<DropboxFolder> parseResponseList(Response response) {
        return responseParser.parseToList(
            listType(),
            response
        );
    }

}
