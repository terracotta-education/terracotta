package edu.iu.terracotta.connectors.generic.service.api;

public interface ApiTokenService {

    boolean findAndDeleteOneUseToken(String token);

}
