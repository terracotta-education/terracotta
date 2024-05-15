package edu.iu.terracotta.model;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lti_context",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "context_key", "deployment_id" })
    }
)
public class LtiContextEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "context_id",
        nullable = false
    )
    private long contextId;

    // per LTI 1.3, the 'Context.id' claim must not be more than 255 characters
    // in length.
    @Column(
        name = "context_key",
        nullable = false,
        length = 255
    )
    private String contextKey;

    @Column(length = 4096)
    private String title;

    @Column(length = 4096)
    private String context_memberships_url;

    @Column(length = 4096)
    private String lineitems;

    @Lob
    @Column
    private String json;

    @Lob
    @Column
    private String settings;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "deployment_id",
        referencedColumnName = "deployment_id",
        nullable = false
    )
    private ToolDeployment toolDeployment;

    @OneToMany(mappedBy = "context")
    private Set<LtiLinkEntity> links;

    @OneToMany(mappedBy = "context")
    private Set<LtiMembershipEntity> memberships;

    public LtiContextEntity(String contextKey, ToolDeployment toolDeployment, String title, String json) {
        if (!StringUtils.isNotBlank(contextKey)) {
            throw new AssertionError();
        }

        if (toolDeployment == null) {
            throw new AssertionError();
        }

        this.contextKey = contextKey;
        this.toolDeployment = toolDeployment;
        this.title = title;
        this.json = json;
    }

    public LtiContextEntity(String contextKey, ToolDeployment toolDeployment, String title, String contextMembershipsUrl, String lineitems, String json) {
        if (!StringUtils.isNotBlank(contextKey)) {
            throw new AssertionError();
        }

        if (toolDeployment == null) {
            throw new AssertionError();
        }

        this.contextKey = contextKey;
        this.toolDeployment = toolDeployment;
        this.title = title;
        this.context_memberships_url = contextMembershipsUrl;
        this.lineitems = lineitems;
        this.json = json;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LtiContextEntity that = (LtiContextEntity) o;

        return contextId == that.contextId || Objects.equals(contextKey, that.contextKey);
    }

    @Override
    public int hashCode() {
        int result = (int) contextId;
        result = 31 * result + (contextKey != null ? contextKey.hashCode() : 0);

        return result;
    }

}
