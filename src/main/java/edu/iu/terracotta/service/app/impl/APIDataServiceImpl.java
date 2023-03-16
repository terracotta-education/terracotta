/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.terracotta.service.app.impl;

import edu.iu.terracotta.repository.AllRepositories;
import edu.iu.terracotta.model.ApiOneUseToken;
import edu.iu.terracotta.service.app.APIDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * This manages all the access to data related with the main APP
 */
@Service
public class APIDataServiceImpl implements APIDataService {

    @Autowired
    private AllRepositories allRepositories;

    @Override
    public void addOneUseToken(String token) {
        allRepositories.apiOneUseTokenRepository.save(new ApiOneUseToken(token));
    }

    @Override
    public boolean findAndDeleteOneUseToken(String token) {
        ApiOneUseToken apiOneUseToken = allRepositories.apiOneUseTokenRepository.findByToken(token);

        if (apiOneUseToken == null) {
            return false;
        }

        allRepositories.apiOneUseTokenRepository.delete(apiOneUseToken);

        return true;
    }

    @Override
    public void cleanOldTokens() {
        allRepositories.apiOneUseTokenRepository.deleteByCreatedAtBefore(new Date(System.currentTimeMillis()-24*60*60*1000));
    }

}
