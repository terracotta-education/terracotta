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

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lti_tool_deployment")
public class ToolDeployment extends BaseEntity {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long deploymentId;

    @Basic
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (deploymentId ^ (deploymentId >>> 32));
        result = prime * result + ((ltiDeploymentId == null) ? 0 : ltiDeploymentId.hashCode());

        return result;
    }

}
