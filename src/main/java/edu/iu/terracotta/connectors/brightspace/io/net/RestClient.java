package edu.iu.terracotta.connectors.brightspace.io.net;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import edu.iu.terracotta.connectors.brightspace.io.exception.InvalidOauthTokenException;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

public interface RestClient {

    Response sendApiGet(@NotNull OauthToken token, @NotNull String url, int connectTimeout, int readTimeout) throws IOException;
    Response sendJsonPost(@NotNull OauthToken token, @NotNull String url, String json, int connectTimeout, int readTimeout) throws IOException;
    Response sendJsonPut(@NotNull OauthToken token, @NotNull String url, String json, int connectTimeout, int readTimeout) throws IOException;
    Response sendApiPost(@NotNull OauthToken token, @NotNull String url, Map<String, List<String>> postParameters, int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException;
    Response sendApiPostFile(@NotNull OauthToken token, @NotNull String url, Map<String, List<String>> postParameters, String fileParameter, String filePath, InputStream is, int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException;
    Response sendApiDelete(@NotNull OauthToken token, @NotNull String url, Map<String, List<String>> deleteParameters, int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException;
    Response sendApiPut(@NotNull OauthToken token, @NotNull String url, Map<String, List<String>> putParameters, int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException;

    String sendUpload(String uploadUrl, Map<String, List<String>> params, InputStream in, String filename, int connectTimeout, int readTimeout) throws IOException;

}
