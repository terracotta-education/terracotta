package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "lti_membership",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "context_id" })
    }
)
public class LtiMembershipEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "membership_id",
        nullable = false
    )
    private long membershipId;

    @Column private Integer role;
    @Column  private Integer roleOverride;

    @JoinColumn(name = "context_id")
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    private LtiContextEntity context;

    @JoinColumn(name = "user_id")
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    private LtiUserEntity user;

    public LtiMembershipEntity(LtiContextEntity context, LtiUserEntity user, Integer role) {
        if (user == null) {
            throw new AssertionError();
        }

        if (context == null) {
            throw new AssertionError();
        }

        this.user = user;
        this.context = context;
        this.role = role;
    }

}
