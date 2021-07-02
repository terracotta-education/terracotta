package edu.iu.terracotta.service.app;

import edu.iu.terracotta.repository.AllRepositories;

public interface APIDataService {
    AllRepositories getRepos();

    void addOneUseToken(String token);

    boolean findAndDeleteOneUseToken(String token);

    void cleanOldTokens();
}
