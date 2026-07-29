package edu.iu.terracotta.connectors.canvas.service.extended.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;

import com.google.common.reflect.TypeToken;

import edu.iu.terracotta.config.ContextProvider;
import edu.iu.terracotta.connectors.canvas.dao.model.extended.UserExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.UserReaderExtended;
import edu.iu.terracotta.connectors.canvas.service.extended.UserWriterExtended;
import edu.ksu.canvas.model.User;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatch;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.iu.terracotta.connectors.generic.service.lms.LmsUserBatchWriteService;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class UserExtendedImpl extends BaseImpl<UserExtended, UserReaderExtended, UserWriterExtended> implements UserReaderExtended, UserWriterExtended {

    private int connectTimeout;
    private int readTimeout;
    private RestClient restClient;
    private JsonMapper jsonMapper = JsonMapper.builder().build();

    public UserExtendedImpl(String canvasBaseUrl, Integer apiVersion, OauthToken oauthToken, RestClient restClient, int connectTimeout, int readTimeout, Integer paginationPageSize, Boolean serializeNulls) {
        super(canvasBaseUrl, apiVersion, oauthToken, restClient, connectTimeout, readTimeout, paginationPageSize, serializeNulls);
        this.restClient = restClient;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.paginationPageSize = paginationPageSize;
    }

    @Override
    public void getUsersInCourse(GetUsersInCourseOptions getUsersInCourseOptions, UUID batchId) throws IOException, InvalidOauthTokenException, TerracottaConnectorException {
        log.debug("Retrieving users in course {}", getUsersInCourseOptions.getCourseId());
        String url = buildCanvasUrl("courses/" + getUsersInCourseOptions.getCourseId() + "/users", getUsersInCourseOptions.getOptionsMap());

        getListFromCanvas(url, batchId);
    }

    private void getListFromCanvas(String url, UUID batchId) throws IOException, InvalidOauthTokenException, TerracottaConnectorException {
        Consumer<Response> consumer = null;

        if (responseCallback != null) {
            consumer = response -> responseCallback.accept(responseParser.parseToList(listType(), response));
        }

        getFromCanvas(oauthToken, url, consumer, batchId);
    }

    private void getFromCanvas(@NotNull OauthToken oauthToken, @NotNull String url, Consumer<Response> callback, UUID batchId) throws InvalidOauthTokenException, IOException, TerracottaConnectorException {
        LmsUserBatchWriteService lmsUserBatchWriteService = ContextProvider.getBean(LmsUserBatchWriteService.class);

        // committed independently of whatever transaction the caller is running in, so progress
        // is visible immediately and durable even if a later page - or the caller's own
        // transaction - fails
        lmsUserBatchWriteService.startBatch(batchId);

        try {
            while (StringUtils.isNotBlank(url)) {
                Response response = getSingleResponseFromCanvas(oauthToken, url);

                if (response.getErrorHappened() || response.getResponseCode() != 200) {
                    String errorMessage = String.format("Errors retrieving responses from canvas for url:  %s", url);
                    log.error(errorMessage);
                    lmsUserBatchWriteService.markFailed(batchId, errorMessage);

                    return;
                }

                List<LmsUserBatch> usersToSave = jsonMapper.readValue(
                    response.getContent(),
                    new TypeReference<List<User>>() {}
                ).stream()
                    .map(
                        user -> LmsUserBatch.builder()
                            .batchId(batchId)
                            .email(user.getEmail())
                            .lmsUserId(String.valueOf(user.getId()))
                            .build()
                    )
                    .toList();

                lmsUserBatchWriteService.saveUsers(usersToSave);

                url = response.getNextLink();

                if (callback != null) {
                    callback.accept(response);
                }
            }
        } catch (IOException | InvalidOauthTokenException e) {
            lmsUserBatchWriteService.markFailed(batchId, e.getMessage());

            throw e;
        } catch (RuntimeException e) {
            lmsUserBatchWriteService.markFailed(batchId, e.getMessage());

            throw e;
        }
    }

    private Response getSingleResponseFromCanvas(@NotNull OauthToken oauthToken, @NotNull String url) throws InvalidOauthTokenException, IOException {
        log.debug("Sending GET request to: {}", url);
        return restClient.sendApiGet(oauthToken, url, connectTimeout, readTimeout);
    }

    @Override
    protected Type listType() {
        return new TypeToken<List<UserExtended>>() {}.getType();
    }

    @Override
    protected Class<UserExtended> objectType() {
        return UserExtended.class;
    }

}
