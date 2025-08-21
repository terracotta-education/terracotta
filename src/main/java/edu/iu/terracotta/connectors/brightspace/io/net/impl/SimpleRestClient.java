package edu.iu.terracotta.connectors.brightspace.io.net.impl;

import edu.iu.terracotta.config.SpringContext;
import edu.iu.terracotta.connectors.brightspace.configuration.BrightspaceConfigurationService;
import edu.iu.terracotta.connectors.brightspace.io.errors.BrightspaceErrorResponse;
import edu.iu.terracotta.connectors.brightspace.io.exception.BrightspaceException;
import edu.iu.terracotta.connectors.brightspace.io.exception.InvalidOauthTokenException;
import edu.iu.terracotta.connectors.brightspace.io.exception.ObjectNotFoundException;
import edu.iu.terracotta.connectors.brightspace.io.impl.ResponseParserServiceImpl;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import edu.ksu.canvas.exception.RateLimitException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.http.MediaType;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@SuppressWarnings({"PMD.GuardLogStatement", "PMD.LooseCoupling"})
public class SimpleRestClient implements RestClient {

    private boolean apiRequestLogEnabled;

    public SimpleRestClient() {
        BrightspaceConfigurationService brightspaceConfigurationService = SpringContext.getBean(BrightspaceConfigurationService.class);
        this.apiRequestLogEnabled = brightspaceConfigurationService.isApiRequestLogEnabled();
    }

    @Override
    public Response sendApiGet(@NotNull OauthToken token, @NotNull String url, int connectTimeout, int readTimeout) throws IOException {
        if (apiRequestLogEnabled) {
            log.debug("Sending GET request to URL: [{}]", url);
        }

        Response response = Response.builder().build();

        try (CloseableHttpClient httpClient = createHttpClient(connectTimeout, readTimeout)) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Authorization", String.format("Bearer %s", token.getAccessToken()));

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
                // deal with the actual content
                response.setContent(handleResponse(httpResponse, httpGet).orElse(null));
                response.setResponseCode(httpResponse.getStatusLine().getStatusCode());

                // deal with pagination
                Header linkHeader = httpResponse.getFirstHeader("Link");
                String linkHeaderValue = linkHeader == null ? null : httpResponse.getFirstHeader("Link").getValue();

                if (linkHeaderValue == null) {
                    return response;
                }

                List<String> links = Arrays.asList(linkHeaderValue.split(","));

                for (String link : links) {
                    if (link.contains("rel=\"next\"")) {
                        String nextLink = link.substring(1, link.indexOf(';') - 1);
                        response.setNext(nextLink);
                    }
                }
            }
        }

        return response;
    }

    @Override
    public Response sendJsonPut(OauthToken token, String url, String json, int connectTimeout, int readTimeout) throws IOException {
        return sendJsonPostOrPut(token, url, json, connectTimeout, readTimeout, "PUT");
    }

    @Override
    public Response sendJsonPost(OauthToken token, String url, String json, int connectTimeout, int readTimeout) throws IOException {
        return sendJsonPostOrPut(token, url, json, connectTimeout, readTimeout, "POST");
    }

    // PUT and POST are identical calls except for the header specifying the method
    private Response sendJsonPostOrPut(OauthToken token, String url, String json, int connectTimeout, int readTimeout, String method) throws IOException {
        if (apiRequestLogEnabled) {
            log.debug("Sending {} request to URL: [{}] with body: [{}]", method, url, json);
        }

        Response response = Response.builder().build();
        HttpClient httpClient = createHttpClient(connectTimeout, readTimeout);
        HttpEntityEnclosingRequestBase action;

        switch (method) {
            case "POST":
                action = new HttpPost(url);
                break;
            case "PUT":
                action = new HttpPut(url);
                break;
            default:
                throw new IllegalArgumentException("Method must be either POST or PUT");
        }

        action.setHeader("Authorization", String.format("Bearer %s", token.getAccessToken()));

        if (StringUtils.isNotBlank(json)) {
            StringEntity requestBody = new StringEntity(json, ContentType.APPLICATION_JSON);
            action.setEntity(requestBody);
        }

        try {
            HttpResponse httpResponse = httpClient.execute(action);
            Optional<String> content = handleResponse(httpResponse, action);

            response.setContent(content.orElse(null));
            response.setResponseCode(httpResponse.getStatusLine().getStatusCode());
        } finally {
            action.releaseConnection();
        }

        return response;
    }

    @Override
    public Response sendApiPost(OauthToken token, String url, Map<String, List<String>> postParameters, int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException {
        if (apiRequestLogEnabled) {
            log.debug("Sending API POST request to URL: [{}]", url);
        }

        HttpClient httpClient = createHttpClient(connectTimeout, readTimeout);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Authorization", String.format("Bearer %s", token.getAccessToken()));
        List<NameValuePair> params = convertParameters(postParameters);
        httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        HttpResponse httpResponse =  httpClient.execute(httpPost);
        Optional<String> content = handleResponse(httpResponse, httpPost);

        return Response.builder()
                .content(content.orElse(null))
                .responseCode(httpResponse.getStatusLine().getStatusCode())
                .build();
    }

    @Override
    public Response sendApiPostFile(OauthToken token, String url, Map<String, List<String>> postParameters, String fileParameter, String filePath, InputStream is, int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException {
        if (apiRequestLogEnabled) {
            log.debug("Sending API POST file request to URL: [{}]", url);
        }

        HttpClient httpClient = createHttpClient(connectTimeout, readTimeout);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Authorization", String.format("Bearer %s", token.getAccessToken()));
        List<NameValuePair> params = convertParameters(postParameters);
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        if(is == null) {
            FileBody fileBody = new FileBody(new File(filePath));
            entityBuilder.addPart(fileParameter, fileBody);
        } else {
            entityBuilder.addPart(fileParameter, new InputStreamBody(is, filePath));
        }

        for(NameValuePair param : params) {
            entityBuilder.addTextBody(param.getName(), param.getValue());
        }

        httpPost.setEntity(entityBuilder.build());
        HttpResponse httpResponse =  httpClient.execute(httpPost);
        Optional<String> content = handleResponse(httpResponse, httpPost);

        return Response.builder()
            .content(content.orElse(null))
            .responseCode(httpResponse.getStatusLine().getStatusCode())
            .build();
    }

    @Override
    public Response sendApiPut(OauthToken token, String url, Map<String, List<String>> putParameters, int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException {
        if (apiRequestLogEnabled) {
            log.debug("Sending API PUT request to URL: [{}]", url);
        }

        HttpClient httpClient = createHttpClient(connectTimeout, readTimeout);
        HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader("Authorization", String.format("Bearer %s", token.getAccessToken()));
        List<NameValuePair> params = convertParameters(putParameters);

        httpPut.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        HttpResponse httpResponse =  httpClient.execute(httpPut);
        Optional<String> content = handleResponse(httpResponse, httpPut);

        return Response.builder()
            .content(content.orElse(null))
            .responseCode(httpResponse.getStatusLine().getStatusCode())
            .build();
    }

    @Override
    public Response sendApiDelete(OauthToken token, String url, Map<String, List<String>> deleteParameters, int connectTimeout, int readTimeout) throws InvalidOauthTokenException, IOException {
        log.debug("Sending API DELETE request to URL: [{}]", url);
        HttpClient httpClient = createHttpClient(connectTimeout, readTimeout);

        // This class is defined here because we need to be able to add form body elements to a delete request for a few api calls.
        class HttpDeleteWithBody extends HttpPost {
            @Override
            public String getMethod() {
                return "DELETE";
            }
        }

        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody();
        httpDelete.setURI(URI.create(url));
        httpDelete.setHeader("Authorization", String.format("Bearer %s", token.getAccessToken()));
        List<NameValuePair> params = convertParameters(deleteParameters);
        httpDelete.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        HttpResponse httpResponse = httpClient.execute(httpDelete);
        Optional<String> content = handleResponse(httpResponse, httpDelete);

        return Response.builder()
            .content(content.orElse(null))
            .responseCode(httpResponse.getStatusLine().getStatusCode())
            .build();
    }

    @Override
    public String sendUpload(String uploadUrl, Map<String, List<String>> params, InputStream in, String filename, int connectTimeout, int readTimeout) throws IOException {
        HttpClient client = buildHttpClient(connectTimeout, readTimeout)
            .disableRedirectHandling() // We need to handle redirects ourselves
            .build();

        HttpPost httpPost = new HttpPost(uploadUrl);
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            for (String value : entry.getValue()) {
                entityBuilder.addTextBody(entry.getKey(), value);
            }
        }
        ContentBody fileBody = new InputStreamBody(in, filename);
        entityBuilder.addPart("file", fileBody);
        httpPost.setEntity(entityBuilder.build());

        HttpResponse httpResponse = client.execute(httpPost);
        checkHeaders(httpResponse, httpPost, true);
        int httpStatus = httpResponse.getStatusLine().getStatusCode();

        if (httpStatus == 201 || (300 <= httpStatus && httpStatus <= 399)) {
            Header location = httpResponse.getFirstHeader("Location");

            if (location != null) {
                return location.getValue();
            } else {
                throw new BrightspaceException(String.format("No location to redirect to when uploading file: %s", httpStatus), uploadUrl);
            }
        } else {
            throw new BrightspaceException(String.format("Bad status when uploading file: %s", httpStatus), uploadUrl);
        }
    }

    private void checkHeaders(HttpResponse httpResponse, HttpRequestBase request, boolean allowRedirect) throws ObjectNotFoundException {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        double rateLimitThreshold = 0.1;
        double xRateCost = 0d;
        double xRateLimitRemaining = 0d;

        try {
            xRateCost = httpResponse.getFirstHeader("X-Request-Cost") != null ? Double.parseDouble(httpResponse.getFirstHeader("X-Request-Cost").getValue()) : 10d;
            xRateLimitRemaining = httpResponse.getFirstHeader("X-Rate-Limit-Remaining") != null ? Double.parseDouble(httpResponse.getFirstHeader("X-Rate-Limit-Remaining").getValue()) : 50000d;

            // Throws a 429 with a "Rate Limit Exceeded" error message if the API throttle limit is hit.
            // See https://community.d2l.com/brightspace/kb/articles/1191-api-rate-logging-limiting-faq.
            if (xRateLimitRemaining < rateLimitThreshold) {
                log.error(
                    "Brightspace API rate limit exceeded. Bucket quota: [{}] Cost: [{}] Threshold: [{}] HTTP status: [{}] Requested URL: [{}]",
                    xRateLimitRemaining,
                    xRateCost,
                    rateLimitThreshold,
                    statusCode,
                    request.getURI()
                );
                throw new RateLimitException(extractErrorMessageFromResponse(httpResponse), String.valueOf(request.getURI()));
            }
        } catch (NullPointerException e) {
            log.debug("Rate not being limited: [{}]", e);
        }

        if (statusCode == 401) {
            if (httpResponse.containsHeader(HttpHeaders.WWW_AUTHENTICATE)) {
                log.debug("User's token is invalid. It might need refreshing");
                throw new InvalidOauthTokenException();
            }

            log.error("User is not authorized to perform this action");
            throw new BrightspaceException("User is not authorized to perform this action", String.valueOf(request.getURI()));
        }

        if (statusCode == 403) {
            log.error("Brightspace has forbidden this request. Requested URL: [{}]", request.getURI());
            throw new BrightspaceException(extractErrorMessageFromResponse(httpResponse), String.valueOf(request.getURI()));
        }

        if (statusCode == 404) {
            log.error("Object not found in Brightspace. Requested URL: [{}]", request.getURI());
            throw new ObjectNotFoundException(extractErrorMessageFromResponse(httpResponse), String.valueOf(request.getURI()));
        }

        if (statusCode == 429) {
            log.error("Brightspace has throttled this request. Requested URL: [{}]", request.getURI());
            throw new BrightspaceException(extractErrorMessageFromResponse(httpResponse), String.valueOf(request.getURI()));
        }

        if (statusCode == 504) {
            log.error("504 Gateway Time-out while requesting: [{}]", request.getURI());
            throw new BrightspaceException("status code: 504, reason phrase: Gateway Time-out", String.valueOf(request.getURI()));
        }

        // If we receive a 5xx exception, we should not wrap it in an unchecked exception for upstream clients to deal with.
        if (statusCode < 200 || (statusCode > (allowRedirect?399:299) && statusCode <= 499)) {
            log.error("HTTP status [{}] returned from [{}]", statusCode, request.getURI());
            handleError(request, httpResponse);
        }
        //TODO Handling of 422 when the entity is malformed.
    }

    private void handleError(HttpRequestBase httpRequest, HttpResponse httpResponse) {
        String brightspaceErrorString = extractErrorMessageFromResponse(httpResponse);
        throw new BrightspaceException(brightspaceErrorString, String.valueOf(httpRequest.getURI()));
    }

    /**
     * Attempts to extract a useful Brightspace error message from a response object.
     *
     * @param response HttpResponse object representing the error response from Brightspace
     * @return The Brightspace human-readable error string or null if unable to extract it
     */
    private String extractErrorMessageFromResponse(HttpResponse response) {
        String contentType = response.getEntity().getContentType().getValue();
        String message = null;

        if (contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            JsonMapper jsonMapper = ResponseParserServiceImpl.getJsonParser(false);
            String responseBody = null;

            try {
                responseBody = EntityUtils.toString(response.getEntity());
                log.error("Body of error response from Brightspace: [{}]", responseBody);
                BrightspaceErrorResponse errorResponse = jsonMapper.readValue(responseBody, BrightspaceErrorResponse.class);
                List<BrightspaceErrorResponse.ErrorMessage> errors = errorResponse.getErrors();

                if (errors != null) {
                    message = errors.stream()
                        .map(BrightspaceErrorResponse.ErrorMessage::getMessage)
                        .collect(Collectors.joining(", "));
                } else {
                    message = responseBody;
                }
            } catch (Exception e) {
                // Returned JSON was not in expected format. Fall back to returning the whole response body, if any
                if (StringUtils.isNotBlank(responseBody)) {
                    message = responseBody;
                }
            }
        }

        return message;
    }

    private Optional<String> handleResponse(HttpResponse httpResponse, HttpRequestBase request) throws IOException {
        try {
            checkHeaders(httpResponse, request, false);
        } catch (ObjectNotFoundException e) {
            log.error(e.getMessage());
            return Optional.empty();
        }

        return Optional.of(new BasicResponseHandler().handleResponse(httpResponse));
    }

    private CloseableHttpClient createHttpClient(int connectTimeout, int readTimeout) {
        return buildHttpClient(connectTimeout, readTimeout).build();
    }

    private HttpClientBuilder buildHttpClient(int connectTimeout, int readTimeout) {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(readTimeout)
            .setCookieSpec(CookieSpecs.STANDARD)
            .build();

        return HttpClientBuilder.create()
            .setDefaultRequestConfig(config);
    }

    private static List<NameValuePair> convertParameters(final Map<String, List<String>> parameterMap) {
        final List<NameValuePair> params = new ArrayList<>();

        if (parameterMap == null) {
            return params;
        }

        for (final Map.Entry<String, List<String>> param : parameterMap.entrySet()) {
            final String key = param.getKey();

            if (param.getValue() == null || param.getValue().isEmpty()) {
                params.add(new BasicNameValuePair(key, null));
                log.debug("key: [{}] empty value", key);
            }

            for (final String value : param.getValue()) {
                params.add(new BasicNameValuePair(key, value));
                log.debug("key: [{}] value: [{}]", key, value);
            }
        }

        return params;
    }

}
