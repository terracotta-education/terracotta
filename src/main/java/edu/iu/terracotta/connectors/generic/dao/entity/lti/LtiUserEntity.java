package edu.iu.terracotta.connectors.generic.dao.entity.lti;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.UuidAwareEntity;
import edu.iu.terracotta.security.pii.EmailHashListener;
import edu.iu.terracotta.security.pii.HashedEmail;
import edu.iu.terracotta.security.pii.PiiStringConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EntityListeners(EmailHashListener.class)
@Table(
    name = "lti_user",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_key", "key_id"}),
    },
    indexes = {
        @Index(name = "IDX_LTI_USER_EMAIL_HASH", columnList = "email_hash, key_id")
    }
)
public class LtiUserEntity extends UuidAwareEntity implements HashedEmail {

    public static final String TEST_STUDENT_DISPLAY_NAME = "Test Student";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "user_id",
        nullable = false
    )
    private long userId;

    // per LTI 1.3 and OIDC specifications, the 'sub' claim must not be more than
    // 255 characters in length.
    @Column(
        name = "user_key",
        nullable = false,
        length = 255
    )
    private String userKey;

    // stored encrypted (PiiCipher) - the length leaves room for the ciphertext of a long name
    @Column(
        name = "displayname",
        length = 8192
    )
    @Convert(converter = PiiStringConverter.class)
    private String displayName;

    @Column(length = 63)
    private String locale;

    @Lob
    @Column
    private String json;

    // stored encrypted (PiiCipher); look a user up by email with emailHash instead
    @Column(length = 512)
    @Convert(converter = PiiStringConverter.class)
    private String email;

    // keyed hash of the email (PiiCipher.hashEmail), kept current by EmailHashListener
    @Column(
        name = "email_hash",
        length = 64
    )
    private String emailHash;

    @Column private String lmsUserId;
    @Column private Short subscribe;
    @Column private Timestamp loginAt;

    @OneToMany(
        mappedBy = "user",
        fetch = FetchType.LAZY
    )
    private Set<LtiResultEntity> results;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "key_id",
        referencedColumnName = "key_id"
    )
    private PlatformDeployment platformDeployment;

    /**
     * @param userKey user identifier
     * @param loginAt date of user login
     */
    public LtiUserEntity(String userKey, Date loginAt, PlatformDeployment platformDeployment1) {
        if (StringUtils.isBlank(userKey)) {
            throw new AssertionError();
        }

        if (loginAt == null) {
            loginAt = new Date();
        }

        if (platformDeployment1 != null) {
            this.platformDeployment = platformDeployment1;
        }

        this.userKey = userKey;
        this.loginAt = new Timestamp(loginAt.getTime());
    }

    @Transient
    public boolean isTestStudent() {
        return Strings.CI.equals(displayName, TEST_STUDENT_DISPLAY_NAME);
    }

}
