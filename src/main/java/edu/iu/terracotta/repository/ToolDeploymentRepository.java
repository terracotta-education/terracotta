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
package edu.iu.terracotta.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import edu.iu.terracotta.model.ToolDeployment;

@Transactional
public interface ToolDeploymentRepository extends JpaRepository<ToolDeployment, Long> {

    List<ToolDeployment> findByPlatformDeployment_Iss(String iss);
    List<ToolDeployment> findByPlatformDeployment_IssAndLtiDeploymentId(String iss, String ltiDeploymentId);
    List<ToolDeployment> findByPlatformDeployment_IssAndPlatformDeployment_ClientId(String iss, String clientId);
    List<ToolDeployment> findByPlatformDeployment_IssAndPlatformDeployment_ClientIdAndLtiDeploymentId(String iss, String clientId, String ltiDeploymentId);
}
