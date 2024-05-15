package edu.iu.terracotta.model.membership;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseUser {

    @JsonProperty("status")
    private String status;

    @JsonProperty("name")
    private String name;

    @JsonProperty("picture")
    private String picture;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("middle_name")
    private String middleName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("lis_person_sourcedid")
    private String lisPersonSourcedid;

    @JsonProperty("lti11_legacy_user_id")
    private String lti11LegacyUserId;

    @JsonProperty("roles")
    private List<String> roles;

}
