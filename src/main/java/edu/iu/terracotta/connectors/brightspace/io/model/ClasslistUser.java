package edu.iu.terracotta.connectors.brightspace.io.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClasslistUser extends BaseBrightspaceModel implements Serializable {

    /*
     {
        "Identifier": <string:D2LID>,
        "ProfileIdentifier": <string>,
        "DisplayName": <string>,
        "Username": <string>|null,
        "OrgDefinedId": <string>|null,
        "Email": <string>|null,
        "FirstName": <string>|null,
        "LastName": <string>|null,
        "RoleId": <number:D2LID>|null,
        "LastAccessed": <string:UTCDateTime>|null,
        "IsOnline": <boolean>,
        "ClasslistRoleDisplayName": <string>,
        "Pronouns": <string>|null  // Added with LMS v20.25.2
     }
     */

    @JsonProperty("Identifier") private String identifier;
    @JsonProperty("ProfileIdentifier") private String profileIdentifier;
    @JsonProperty("DisplayName") private String displayName;
    @JsonProperty("Username") private String username;
    @JsonProperty("OrgDefinedId") private String orgDefinedId;
    @JsonProperty("Email") private String email;
    @JsonProperty("FirstName") private String firstName;
    @JsonProperty("LastName") private String lastName;
    @JsonProperty("RoleId") private Long roleId;
    @JsonProperty("LastAccessed") private String lastAccessed;
    @JsonProperty("IsOnline") private Boolean isOnline;
    @JsonProperty("ClasslistRoleDisplayName") private String classlistRoleDisplayName;
    @JsonProperty("Pronouns") private String pronouns;

}
