package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.repository.ApiOneUseTokenRepository;
import edu.iu.terracotta.model.ApiOneUseToken;
import edu.iu.terracotta.service.app.APIDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This manages all the access to data related with the main APP
 */
@Service
public class APIDataServiceImpl implements APIDataService {

    @Autowired private ApiOneUseTokenRepository apiOneUseTokenRepository;

    @Override
    public boolean findAndDeleteOneUseToken(String token) {
        ApiOneUseToken apiOneUseToken = apiOneUseTokenRepository.findByToken(token);

        if (apiOneUseToken == null) {
            return false;
        }

        apiOneUseTokenRepository.delete(apiOneUseToken);

        return true;
    }

}
