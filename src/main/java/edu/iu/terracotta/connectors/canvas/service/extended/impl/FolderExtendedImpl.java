package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.reflect.TypeToken;

import edu.iu.terracotta.connectors.canvas.dao.model.extended.FolderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.FolderReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.FolderWriterExtended;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;

public class FolderExtendedImpl  extends BaseImpl<FolderExtended, FolderReaderExtended, FolderWriterExtended> implements FolderReaderExtended, FolderWriterExtended {

    public FolderExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public Optional<FolderExtended> getFolder(String id) throws IOException {
        String url = buildCanvasUrl(
            String.format(
                "users/self/folders/%s",
                id
            ),
            Collections.emptyMap()
        );

        return parseResponseObjectOptional(canvasMessenger.getSingleResponseFromCanvas(oauthToken, url));
    }

    @Override
    public List<FolderExtended> getFolders() throws IOException {
        String url = buildCanvasUrl(
            "users/self/folders/by_path/conversation%20attachments",
            Collections.emptyMap()
        );

        return parseList(getListResponseFromCanvas(url));
    }

    @Override
    protected Type listType() {
        return new TypeToken<List<FolderExtended>>() {}.getType();
    }

    @Override
    protected Class<FolderExtended> objectType() {
        return FolderExtended.class;
    }

    private List<FolderExtended> parseList(List<Response> responses) {
        List<FolderExtended> folderExtendedList = new ArrayList<>();

        responses.stream()
            .forEach(response -> folderExtendedList.addAll(parseResponseList(response)));

        return folderExtendedList;
    }

    private List<FolderExtended> parseResponseList(Response response) {
        return responseParser.parseToList(
            listType(),
            response
        );
    }

    private List<Response> getListResponseFromCanvas(String url) throws IOException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        List<Response> responses = canvasMessenger.getFromCanvas(oauthToken, url, consumer);
        responseCallback = null;

        return responses;
    }

    private Optional<FolderExtended> parseResponseObjectOptional(Response response) {
        return responseParser.parseToObject(
            objectType(),
            response
        );
    }

}
