package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;

import edu.iu.terracotta.connectors.brightspace.io.interfaces.UserGradeValueReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.UserGradeValueWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValue;
import edu.iu.terracotta.connectors.brightspace.io.model.UserGradeValuePaged;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceUrl;
import edu.iu.terracotta.connectors.brightspace.io.net.RestClient;
import edu.iu.terracotta.connectors.brightspace.io.net.model.Response;
import edu.iu.terracotta.connectors.brightspace.io.oauth.OauthToken;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class UserGradeValueServiceImpl extends BaseServiceImpl<UserGradeValue, UserGradeValueReaderService, UserGradeValueWriterService> implements UserGradeValueReaderService, UserGradeValueWriterService {

    public UserGradeValueServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<UserGradeValue> getAll(String orgUnitId, String gradeObjectId) throws IOException {
        return parseList(
            getListResponse(
                buildUrl(
                    String.format(
                        BrightspaceUrl.GRADE_VALUES.url(),
                        apiVersion.getLe(),
                        orgUnitId,
                        gradeObjectId
                    ),
                    Map.of()
                )
            )
        );
    }

    @Override
    protected TypeReference<List<UserGradeValue>> listType() {
        return new TypeReference<List<UserGradeValue>>() {};
    }

    @Override
    protected Class<UserGradeValue> objectType() {
        return UserGradeValue.class;
    }


    private List<Response> getListResponse(String url) throws IOException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        List<Response> responses = brightspaceMessenger.get(oauthToken, url, consumer);
        responseCallback = null;

        JsonMapper jsonMapper = ResponseParserServiceImpl.getJsonParser(serializeNulls);

        responses = CollectionUtils.emptyIfNull(responses).stream()
            .map(
                response -> {
                    Optional<UserGradeValuePaged> userGradeValuePaged = responseParser.parseToObject(UserGradeValuePaged.class, response);

                    try {
                        response.setContent(jsonMapper.writeValueAsString(userGradeValuePaged.map(UserGradeValuePaged::getObjects).orElse(List.of())));
                    } catch (JacksonException e) {
                        log.error("Error processing JSON for response body for URL: [{}]", url, e);
                        return null;
                    }

                    return response;
                }
            )
            .filter(Objects::nonNull)
            .toList();

        return responses;
    }

    private List<UserGradeValue> parseList(List<Response> responses) {
        List<UserGradeValue> userGradeValueList = new ArrayList<>();

        responses.stream()
            .forEach(response -> userGradeValueList.addAll(parseResponseList(response)));

        return userGradeValueList;
    }

    private List<UserGradeValue> parseResponseList(Response response) {
        return responseParser.parseToList(
            listType(),
            response
        );
    }

}
