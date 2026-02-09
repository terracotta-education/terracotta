package edu.iu.terracotta.connectors.brightspace.io.oauth;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NonRefreshableOauthToken implements OauthToken {

    private static final long serialVersionUID = 1L;

    private String token;

    @Override
    public String getAccessToken() {
        return token;
    }

    @Override
    public void refresh() {
        // No-op for non-refreshable tokens
    }

}
