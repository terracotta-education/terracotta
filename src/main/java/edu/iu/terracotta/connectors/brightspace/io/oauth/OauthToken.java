package edu.iu.terracotta.connectors.brightspace.io.oauth;

import java.io.Serializable;

public interface OauthToken extends Serializable {

    String getAccessToken();
    void refresh();

}
