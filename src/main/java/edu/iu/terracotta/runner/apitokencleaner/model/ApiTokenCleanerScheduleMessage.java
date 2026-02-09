package edu.iu.terracotta.runner.apitokencleaner.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import edu.iu.terracotta.connectors.generic.dao.model.enums.LmsConnector;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApiTokenCleanerScheduleMessage {

    private long id;
    private String lmsUserName;
    private long userId;
    private LmsConnector lmsConnector;
    private String accessToken;
    private String refreshToken;
    private Timestamp expiresAt;
    private Timestamp deletedAt;
    private List<String> errors;

    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }

        errors.add(error);
    }

}
