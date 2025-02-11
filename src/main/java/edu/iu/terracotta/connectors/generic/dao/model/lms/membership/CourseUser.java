package edu.iu.terracotta.connectors.generic.dao.model.lms.membership;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseUser {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("lis_person_sourcedid")
    private String lisPersonSourcedid;

    @JsonProperty("lti11_legacy_user_id")
    private String lti11LegacyUserId;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("middle_name")
    private String middleName;

    @JsonProperty private String status;
    @JsonProperty private String name;
    @JsonProperty private String picture;
    @JsonProperty private String email;
    @JsonProperty private List<String> roles;

}
