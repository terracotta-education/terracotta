package edu.iu.terracotta.service.app;

public interface APIDataService {

    void addOneUseToken(String token);

    boolean findAndDeleteOneUseToken(String token);

    void cleanOldTokens();

}
