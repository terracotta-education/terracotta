package edu.iu.terracotta.connectors.brightspace.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.collections4.CollectionUtils;
import edu.iu.terracotta.connectors.brightspace.dao.model.extended.UserExtended;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ClasslistUserReaderService;
import edu.iu.terracotta.connectors.brightspace.io.interfaces.ClasslistUserWriterService;
import edu.iu.terracotta.connectors.brightspace.io.model.ApiVersion;
import edu.iu.terracotta.connectors.brightspace.io.model.ClasslistUser;
import edu.iu.terracotta.connectors.brightspace.io.model.ClasslistUserPaged;
import edu.iu.terracotta.connectors.brightspace.io.model.enums.api.BrightspaceQueryParam;
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
public class ClasslistUserServiceImpl extends BaseServiceImpl<ClasslistUser, ClasslistUserReaderService, ClasslistUserWriterService> implements ClasslistUserReaderService, ClasslistUserWriterService {

    public ClasslistUserServiceImpl(String brightspaceBaseUrl, ApiVersion apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(brightspaceBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
    }

    @Override
    public List<UserExtended> getAll(String orgUnitId, boolean onlyShowShownInGrades, Long roleId) throws IOException {
        List<ClasslistUser> classlistUsers = parseList(
            getListResponse(
                buildUrl(
                    String.format(
                        BrightspaceUrl.ENROLLMENTS.url(),
                        apiVersion.getLe(),
                        orgUnitId
                    ),
                    Map.of(
                        BrightspaceQueryParam.ONLY_SHOW_SHOWN_IN_GRADES.key(),
                        List.of(
                            Boolean.toString(onlyShowShownInGrades)
                        ),
                        BrightspaceQueryParam.ROLE_ID.key(),
                        roleId != null ? List.of(Long.toString(roleId)) : List.of()
                    )
                )
            )
        );

        return classlistUsers.stream()
            .map(
                classlistUser -> {
                    UserExtended userExtended = UserExtended.builder().build();
                    userExtended.setClasslistUser(classlistUser);

                    return userExtended;
                }
            )
            .toList();
    }

    @Override
    protected TypeReference<List<ClasslistUser>> listType() {
        return new TypeReference<List<ClasslistUser>>() {};
    }

    @Override
    protected Class<ClasslistUser> objectType() {
        return ClasslistUser.class;
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
                    Optional<ClasslistUserPaged> classlistUserPaged = responseParser.parseToObject(ClasslistUserPaged.class, response);

                    try {
                        response.setContent(jsonMapper.writeValueAsString(classlistUserPaged.map(ClasslistUserPaged::getObjects).orElse(List.of())));
                    } catch (JacksonException e) {
                        log.error("Error processing JSON for response body for URL: {}", url, e);
                        return null;
                    }

                    return response;
                }
            )
            .filter(Objects::nonNull)
            .toList();

        return responses;
    }

    private List<ClasslistUser> parseList(List<Response> responses) {
        List<ClasslistUser> classlistUserList = new ArrayList<>();

        responses.stream()
            .forEach(response -> classlistUserList.addAll(parseResponseList(response)));

        return classlistUserList;
    }

    private List<ClasslistUser> parseResponseList(Response response) {
        return responseParser.parseToList(
            listType(),
            response
        );
    }

}
