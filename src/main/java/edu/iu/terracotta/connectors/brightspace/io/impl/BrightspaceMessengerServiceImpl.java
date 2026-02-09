package edu.iu.terracotta.connectors.brightspace.io.impl;

import edu.iu.terracotta.connectors.brightspace.io.exception.InvalidOauthTokenException;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@SuppressWarnings("{PMD.GuardLogStatement}")
public class BrightspaceMessengerServiceImpl implements BrightspaceMessengerService {

    private RestClient restClient;
    private int connectTimeout;
    private int readTimeout;

    public BrightspaceMessengerServiceImpl(int connectTimeout, int readTimeout, RestClient restClient) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.restClient = restClient;
    }

    @Override
    public List<Response> get(@NotNull OauthToken oauthToken, @NotNull String url) throws InvalidOauthTokenException, IOException {
        return get(oauthToken, url, null);
    }

    @Override
    public List<Response> get(@NotNull OauthToken oauthToken, @NotNull String url, Consumer<Response> callback) throws InvalidOauthTokenException, IOException {
        List<Response> responses = new ArrayList<>();

        while (StringUtils.isNotBlank(url)) {
            Response response = getSingleResponse(oauthToken, url);

            if (response.isErrorHappened() || response.getResponseCode() != 200) {
                log.error("Errors retrieving responses from brightspace for url: [{}]", url);
                return Collections.emptyList();
            }

            responses.add(response);
            url = response.getNext();

            if (callback != null) {
                callback.accept(response);
            }
        }

        return responses;
    }

    @Override
    public Response send(@NotNull OauthToken oauthToken, @NotNull String url) throws InvalidOauthTokenException, IOException {
        return send(oauthToken, url, Map.of());
    }

    @Override
    public Response send(@NotNull OauthToken oauthToken, @NotNull String url, @NotNull Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException {
        return restClient.sendApiPost(oauthToken, url, parameters, connectTimeout, readTimeout);
    }

    @Override
    public Response sendFile(@NotNull OauthToken oauthToken, @NotNull String url, String fileParameter, String filePath, InputStream is) throws InvalidOauthTokenException, IOException {
        return sendFile(oauthToken, url, Map.of(), fileParameter, filePath, is);
    }

    @Override
    public Response sendFile(@NotNull OauthToken oauthToken, @NotNull String url, @NotNull Map<String, List<String>> parameters, String fileParameter, String filePath, InputStream is) throws InvalidOauthTokenException, IOException {
        return restClient.sendApiPostFile(oauthToken, url, parameters, fileParameter, filePath, is, connectTimeout, readTimeout);
    }

    @Override
    public Response sendJsonPost(OauthToken oauthToken, String url, String requestBody) throws InvalidOauthTokenException, IOException {
        return restClient.sendJsonPost(oauthToken, url, requestBody, connectTimeout, readTimeout);
    }

    @Override
    public Response sendJsonPut(OauthToken oauthToken, String url, String requestBody) throws InvalidOauthTokenException, IOException {
        return restClient.sendJsonPut(oauthToken, url, requestBody, connectTimeout, readTimeout);
    }

    @Override
    public Response delete(@NotNull OauthToken oauthToken, @NotNull String url) throws InvalidOauthTokenException, IOException {
        return delete(oauthToken, url, Map.of());
    }

    @Override
    public Response delete(@NotNull OauthToken oauthToken, @NotNull String url, @NotNull Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException {
        return restClient.sendApiDelete(oauthToken, url, parameters, connectTimeout, readTimeout);
    }

    @Override
    public Response put(@NotNull OauthToken oauthToken, @NotNull String url) throws InvalidOauthTokenException, IOException {
        return put(oauthToken, url, Map.of());
    }

    @Override
    public Response put(@NotNull OauthToken oauthToken, @NotNull String url, @NotNull Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException {
        return restClient.sendApiPut(oauthToken, url, parameters, connectTimeout, readTimeout);
    }

    @Override
    public Response getSingleResponse(@NotNull OauthToken oauthToken, @NotNull String url) throws InvalidOauthTokenException, IOException {
        return restClient.sendApiGet(oauthToken, url, connectTimeout, readTimeout);
    }

}
