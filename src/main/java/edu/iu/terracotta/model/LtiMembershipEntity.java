package edu.iu.terracotta.model;

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

import java.util.Objects;

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

    @Column
    private Integer role;

    @Column
    private Integer roleOverride;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LtiMembershipEntity that = (LtiMembershipEntity) o;

        if (context.getContextId() != that.context.getContextId()) {
            return false;
        }

        if (membershipId != that.membershipId) {
            return false;
        }

        return user.getUserId() == that.user.getUserId() ||  Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        int result = (int) membershipId;
        result = 31 * result + (int) context.getContextId();
        result = 31 * result + (int) user.getUserId();
        result = 31 * result + (role != null ? role.hashCode() : 0);

        return result;
    }

}
