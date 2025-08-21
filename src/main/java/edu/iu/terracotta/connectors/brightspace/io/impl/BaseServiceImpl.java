package edu.iu.terracotta.connectors.brightspace.io.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.net.UrlEscapers;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceMessengerService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.BrightspaceWriterService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ResponseParserService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import edu.iu.terracotta.connectors.brightspace.io.util.BrightspaceUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Base class for accessing the Brightspace API
 */

@Slf4j
@SuppressWarnings({"unchecked", "rawtypes", "PMD.GuardLogStatement", "PMD.LooseCoupling", "PMD.UnusedPrivateMethod"})
public abstract class BaseServiceImpl<T, R extends BrightspaceReaderService, W extends BrightspaceWriterService> implements BrightspaceReaderService<T, R>, BrightspaceWriterService<T, W> {

    protected String baseUrl;
    protected ApiVersion apiVersion;
    protected OauthToken oauthToken;
    protected ResponseParserService responseParser;
    protected BrightspaceMessengerService brightspaceMessenger;
    protected Consumer<List<T>> responseCallback;
    protected Integer paginationPageSize;
    protected Boolean serializeNulls = false;

    /**
     * Construct a new BrightspaceApi class with an OAuth token
     *
     * @param brightspaceBaseUrl The base URL of your Brightspace instance
     * @param apiVersion The version of the Brightspace API
     * @param oauthToken OAuth token to use when executing API calls
     * @param restClient a RestClient implementation to use when talking to Brightspace
     * @param connectTimeout Timeout in seconds to use when connecting
     * @param readTimeout Timeout in seconds to use when waiting for data to come back from an open connection
     * @param paginationPageSize How many objects to request per page on paginated requests
     * @param serializeNulls Whether or not to include null fields in the serialized JSON. Defaults to false if null
     */
    public BaseServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        this.baseUrl = brightspaceBaseUrl;
        this.apiVersion = apiVersion;
        this.oauthToken = oauthToken;
        this.paginationPageSize = paginationPageSize;
        this.serializeNulls = BooleanUtils.isTrue(serializeNulls);
        responseParser = new ResponseParserServiceImpl();
        brightspaceMessenger = new BrightspaceMessengerServiceImpl(connectTimeout, readTimeout, restClient);
    }

    protected Optional<T> get(String url) throws IOException {
        Response response = brightspaceMessenger.getSingleResponse(oauthToken, url);

        if (response.isErrorHappened() || response.getResponseCode() != 200) {
            log.warn("Error [{}] on GET from url [{}]", response.getResponseCode(), url);
            throw new IOException(String.format("Error accessing url [%s]", url));
        }
        return responseParser.parseToObject(objectType(), response);
    }

    protected List<T> getList(String url) throws IOException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        List<Response> responses = brightspaceMessenger.get(oauthToken, url, consumer);
        responseCallback = null;

        return parseListOfResponses(responses);
    }

    @Override
    public R withCallback(Consumer<List<T>> responseReceivedCallBack) {
        responseCallback = responseReceivedCallBack;

        return (R) this;
    }

    protected String encode(String value) {
        return UrlEscapers.urlPathSegmentEscaper().escape(value);
    }

    protected String buildUrl(String path) {
        return buildUrl(path, Map.of());
    }

    protected String buildUrl(String path, Map<String, List<String>> parameters) {
        Map<String, List<String>> allParameters = new HashMap<>();
        allParameters.putAll(parameters);

        Map<String, List<String>> nonEmptyParams = stripEmptyParams(allParameters);

        return BrightspaceUrlBuilder.buildUrl(baseUrl, path, nonEmptyParams);
    }

    private Map<String, List<String>> stripEmptyParams(Map<String, List<String>> parameters) {
        if (MapUtils.isEmpty(parameters)) {
            return Map.of();
        }

        Builder<String, List<String>> paramsBuilder = ImmutableMap.<String, List<String>>builder();

        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            if (CollectionUtils.isNotEmpty(entry.getValue())) {
                paramsBuilder.put(entry.getKey(), entry.getValue());
            }
        }

        return paramsBuilder.build();
    }

    private List<T> parseListResponse(Response response) {
        return responseParser.parseToList(listType(), response);
    }

    /*
     * Subclasses should return the type of a list that will be parsed by jackson when using call that returns lists.
     * For example, GradeObjectReaderImpl returns 'TypeReference<List<GradeObject>>(){}'
     */
    protected abstract TypeReference<List<T>> listType();

    /*
     * Subclasses should return the type of model that will be parsed by jackson when using a call that returns a single object. For example, GradeObjectReaderImpl returns GradeObject.class.
     */
    protected abstract Class<T> objectType();

    protected List<T> parseListOfResponses(List<Response> responses) {
        return responses.stream()
            .map(this::parseListResponse)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

}
