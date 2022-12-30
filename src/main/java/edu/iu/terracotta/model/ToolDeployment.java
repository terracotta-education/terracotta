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
package edu.iu.terracotta.model;

import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "lti_tool_deployment")
public class ToolDeployment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deployment_id")
    private long deploymentId;

    @Basic
    @Column(name = "lti_deployment_id", nullable = false)
    private String ltiDeploymentId;

    @JsonIgnore
    @JoinColumn(name = "key_id", nullable = false)
    @ManyToOne(optional = false)
    private PlatformDeployment platformDeployment;

    @JsonIgnore
    @OneToMany(mappedBy = "toolDeployment", fetch = FetchType.LAZY)
    private Set<LtiContextEntity> contexts;

    public long getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(long deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getLtiDeploymentId() {
        return ltiDeploymentId;
    }

    public void setLtiDeploymentId(String ltiDeploymentId) {
        this.ltiDeploymentId = ltiDeploymentId;
    }

    public PlatformDeployment getPlatformDeployment() {
        return platformDeployment;
    }

    public void setPlatformDeployment(PlatformDeployment platformDeployment) {
        this.platformDeployment = platformDeployment;
    }

    public Set<LtiContextEntity> getContexts() {
        return contexts;
    }

    public void setContexts(Set<LtiContextEntity> contexts) {
        this.contexts = contexts;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (deploymentId ^ (deploymentId >>> 32));
        result = prime * result + ((ltiDeploymentId == null) ? 0 : ltiDeploymentId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ToolDeployment other = (ToolDeployment) obj;
        if (deploymentId != other.deploymentId)
            return false;
        if (ltiDeploymentId == null) {
            if (other.ltiDeploymentId != null)
                return false;
        } else if (!ltiDeploymentId.equals(other.ltiDeploymentId))
            return false;
        return true;
    }

}
