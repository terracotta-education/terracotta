package edu.iu.terracotta.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.connectors.generic.dao.entity.lti.LtiUserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "terr_admin_user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminUser extends BaseEntity  {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long adminUserId;

    @Column
    private boolean enabled;

    @OneToOne(optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    private LtiUserEntity ltiUserEntity;

}
