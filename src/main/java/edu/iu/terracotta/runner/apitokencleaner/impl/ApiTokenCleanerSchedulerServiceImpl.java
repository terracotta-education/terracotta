package edu.iu.terracotta.runner.apitokencleaner.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiTokenEntity;
import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiTokenRepository;
import edu.iu.terracotta.runner.apitokencleaner.ApiTokenCleanerSchedulerService;
import edu.iu.terracotta.runner.apitokencleaner.model.ApiTokenCleanerScheduleMessage;
import edu.iu.terracotta.runner.apitokencleaner.model.ApiTokenCleanerScheduleResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SuppressWarnings({"PMD.GuardLogStatement"})
public class ApiTokenCleanerSchedulerServiceImpl implements ApiTokenCleanerSchedulerService {

    @Autowired private ApiTokenRepository apiTokenRepository;

    @Override
    public Optional<ApiTokenCleanerScheduleResult> cleanup(int expirationTtlDays) {
        List<ApiTokenEntity> apiTokensToCheck = apiTokenRepository.findAllByLmsConnector(LmsConnector.BRIGHTSPACE).stream()
            .filter(apiToken -> apiToken.getExpiresAt().before(Timestamp.valueOf(LocalDateTime.now().minusDays(expirationTtlDays))))
            .toList();

        if (CollectionUtils.isEmpty(apiTokensToCheck)) {
            // no API tokens exist; exit
            return Optional.empty();
        }

        return Optional.of(
            ApiTokenCleanerScheduleResult.builder()
                .processed(
                    apiTokensToCheck.stream()
                        .map(
                            apiToken -> {
                                String error = null;

                                try {
                                    // perform deletion
                                    apiTokenRepository.delete(apiToken);
                                } catch (Exception e) {
                                    log.warn("Error deleting expired API token with id [{}]: {}", apiToken.getTokenId(), e.getMessage());
                                    error = e.getMessage();
                                }

                                return ApiTokenCleanerScheduleMessage.builder()
                                    .accessToken(apiToken.getAccessToken())
                                    .deletedAt(Timestamp.valueOf(LocalDateTime.now()))
                                    .errors(StringUtils.isEmpty(error) ? null : List.of(error))
                                    .expiresAt(apiToken.getExpiresAt())
                                    .id(apiToken.getTokenId())
                                    .lmsConnector(apiToken.getLmsConnector())
                                    .lmsUserName(apiToken.getLmsUserName())
                                    .refreshToken(apiToken.getRefreshToken())
                                    .userId(apiToken.getUser().getUserId())
                                    .build();
                            }
                        )
                        .toList()
                )
                .build()
        );
    }

}
