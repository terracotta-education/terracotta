package edu.iu.terracotta.connectors.brightspace.dao.model.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BrightspaceApiUser {

    @JsonProperty("Identifier") private String identifier;
    @JsonProperty("FirstName") private String firstName;
    @JsonProperty("LastName") private String lastName;
    @JsonProperty("Pronouns") private String pronouns;
    @JsonProperty("UniqueName") private String uniqueName;
    @JsonProperty("ProfileIdentifier") private String profileIdentifier;

}
