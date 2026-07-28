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
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchProcessing;
import edu.iu.terracotta.connectors.generic.dao.entity.lms.LmsUserBatchStatus;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchProcessingRepository;
import edu.iu.terracotta.connectors.generic.dao.repository.lms.LmsUserBatchRepository;
import edu.iu.terracotta.connectors.generic.exceptions.TerracottaConnectorException;
import edu.ksu.canvas.exception.InvalidOauthTokenException;
import edu.ksu.canvas.impl.BaseImpl;
import edu.ksu.canvas.net.Response;
import edu.ksu.canvas.net.RestClient;
import edu.ksu.canvas.oauth.OauthToken;
import edu.ksu.canvas.requestOptions.GetUsersInCourseOptions;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@SuppressWarnings({"PMD.GuardLogStatement"})
public class UserExtendedImpl extends BaseImpl<UserExtended, UserReaderExtended, UserWriterExtended> implements UserReaderExtended, UserWriterExtended {

    private EntityManager entityManager;

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
        this.entityManager = ContextProvider.getEntityManager();
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
        LmsUserBatchRepository lmsUserBatchRepository = ContextProvider.getBean(LmsUserBatchRepository.class);
        LmsUserBatchProcessingRepository lmsUserBatchProcessingRepository = ContextProvider.getBean(LmsUserBatchProcessingRepository.class);

        LmsUserBatchProcessing lmsUserBatchProcessing = lmsUserBatchProcessingRepository.saveAndFlush(
            LmsUserBatchProcessing.builder()
                .batchId(batchId)
                .status(LmsUserBatchStatus.IN_PROGRESS)
                .build()
        );

        while (StringUtils.isNotBlank(url)) {
            Response response = getSingleResponseFromCanvas(oauthToken, url);

            if (response.getErrorHappened() || response.getResponseCode() != 200) {
                String errorMessage = String.format("Errors retrieving responses from canvas for url:  %s", url);
                log.error(errorMessage);

                lmsUserBatchProcessing = lmsUserBatchProcessingRepository.findByBatchId(batchId)
                    .orElse(LmsUserBatchProcessing.builder().batchId(batchId).build());
                lmsUserBatchProcessing.setStatus(LmsUserBatchStatus.FAILED);
                lmsUserBatchProcessing.setMessage(errorMessage);
                lmsUserBatchProcessingRepository.saveAndFlush(lmsUserBatchProcessing);

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

            lmsUserBatchRepository.saveAll(usersToSave);

            entityManager.flush();
            entityManager.clear();

            url = response.getNextLink();

            if (callback != null) {
                callback.accept(response);
            }
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
