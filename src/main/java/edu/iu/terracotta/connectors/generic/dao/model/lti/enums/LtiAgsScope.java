package edu.iu.terracotta.connectors.generic.dao.model.lti.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LtiAgsScope {

    AGS_LINEITEM("https://purl.imsglobal.org/spec/lti-ags/scope/lineitem"),
    AGS_RESULT_READONLY("https://purl.imsglobal.org/spec/lti-ags/scope/result.readonly"),
    AGS_SCORE("https://purl.imsglobal.org/spec/lti-ags/scope/score"),
    DELETE_RESULTS("deleteResults"),
    LINEITEM("lineitem"),
    LINEITEMS("lineitems"),
    LIS_LINEITEM_CONTAINER_JSON("application/vnd.ims.lis.v2.lineitemcontainer+json"),
    LIS_LINEITEM_JSON("application/vnd.ims.lis.v2.lineitem+json"),
    NRPS_MEMBERSHIP_JSON_ACCEPT("application/vnd.ims.lti-nrps.v2.membershipcontainer+json"),
    NRPS_MEMBERSHIP_READONLY("https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly"),
    RESULTS("results"),
    SCORE("score"),
    SCORES("scores"),
    SINGLE("single");

    private String key;

    public String key() {
        return key;
    }

}
