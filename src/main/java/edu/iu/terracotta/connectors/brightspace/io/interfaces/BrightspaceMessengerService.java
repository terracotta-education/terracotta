package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import edu.iu.terracotta.connectors.brightspace.io.exception.InvalidOauthTokenException;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;

@SuppressWarnings({"PMD.LooseCoupling"})
public interface BrightspaceMessengerService {

    List<Response> get(OauthToken oauthToken, String url) throws InvalidOauthTokenException, IOException;
    List<Response> get(OauthToken oauthToken, String url, Consumer<Response> consumer) throws InvalidOauthTokenException, IOException;
    Response send(OauthToken oauthToken, String url) throws InvalidOauthTokenException, IOException;
    Response send(OauthToken oauthToken, String url, Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException;
    Response sendFile(OauthToken oauthToken, String url, String fileParameter, String filePath, InputStream is) throws InvalidOauthTokenException, IOException;
    Response sendFile(OauthToken oauthToken, String url, Map<String, List<String>> parameters, String fileParameter, String filePath, InputStream is) throws InvalidOauthTokenException, IOException;
    Response sendJsonPost(OauthToken oauthToken, String url, String requestBody) throws InvalidOauthTokenException, IOException;
    Response sendJsonPut(OauthToken oauthToken, String url, String requestBody) throws InvalidOauthTokenException, IOException;
    Response delete(@NotNull OauthToken oauthToken, @NotNull String url) throws InvalidOauthTokenException, IOException;
    Response delete(OauthToken oauthToken, String url, Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException;
    Response getSingleResponse(OauthToken oauthToken, String url) throws InvalidOauthTokenException, IOException;
    Response put(OauthToken oauthToken, String url) throws InvalidOauthTokenException, IOException;
    Response put(OauthToken oauthToken, String url, Map<String, List<String>> parameters) throws InvalidOauthTokenException, IOException;

}
