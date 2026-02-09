package edu.iu.terracotta.connectors.brightspace.io.model;

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
public class User {

    /*
     {
        "Identifier": <string>|null,
        "DisplayName": <string>|null,
        "EmailAddress": <string>|null,
        "OrgDefinedId": <string>|null,
        "ProfileBadgeUrl": <string:APIURL>|null,
        "ProfileIdentifier": <string>|null,
        "UserName": <string>|null
     }
     */

    @JsonProperty("Identifier") private String identifier;
    @JsonProperty("DisplayName") private String displayName;
    @JsonProperty("EmailAddress") private String emailAddress;
    @JsonProperty("OrgDefinedId") private String orgDefinedId;
    @JsonProperty("ProfileBadgeUrl") private String profileBadgeUrl;
    @JsonProperty("ProfileIdentifier") private String profileIdentifier;
    @JsonProperty("UserName") private String userName;

}
