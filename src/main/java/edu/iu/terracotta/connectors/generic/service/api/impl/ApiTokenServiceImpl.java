package edu.iu.terracotta.connectors.generic.service.api.impl;

import edu.iu.terracotta.connectors.generic.dao.entity.api.ApiOneUseToken;
import edu.iu.terracotta.connectors.generic.dao.repository.api.ApiOneUseTokenRepository;
import edu.iu.terracotta.connectors.generic.service.api.ApiTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This manages all the access to data related with the main APP
 */
@Service
public class ApiTokenServiceImpl implements ApiTokenService {

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
