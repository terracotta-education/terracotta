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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "lti_tool_deployment")
public class ToolDeployment extends BaseEntity {

    @Id
    @Column(name = "deployment_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long deploymentId;

    @Column(nullable = false)
    private String ltiDeploymentId;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "key_id", nullable = false)
    private PlatformDeployment platformDeployment;

    @JsonIgnore
    @OneToMany(mappedBy = "toolDeployment", fetch = FetchType.LAZY)
    private Set<LtiContextEntity> contexts;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (deploymentId ^ (deploymentId >>> 32));
        return prime * result + ((ltiDeploymentId == null) ? 0 : ltiDeploymentId.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        ToolDeployment other = (ToolDeployment) obj;

        if (deploymentId != other.deploymentId) {
            return false;
        }

        if (ltiDeploymentId == null) {
            if (other.ltiDeploymentId != null) {
                return false;
            }
        } else if (!ltiDeploymentId.equals(other.ltiDeploymentId)) {
            return false;
        }

        return true;
    }

}
