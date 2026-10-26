package edu.iu.terracotta.connectors.generic.dao.entity.lms;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.entity.BaseEntity;
import edu.iu.terracotta.security.pii.EmailHashListener;
import edu.iu.terracotta.security.pii.HashedEmail;
import edu.iu.terracotta.security.pii.PiiStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
@EntityListeners(EmailHashListener.class)
@Table(
    name = "lms_user_batch",
    indexes = {
        @Index(name = "IDX_LMS_USER_BATCH_EMAIL_HASH", columnList = "batch_id, email_hash")
    }
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LmsUserBatch extends BaseEntity implements HashedEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID batchId;
    private String userKey;

    // stored encrypted (PiiCipher); match a staged user by email with emailHash instead
    @Column(length = 1024)
    @Convert(converter = PiiStringConverter.class)
    private String email;

    // keyed hash of the email (PiiCipher.hashEmail), kept current by EmailHashListener
    @Column(
        name = "email_hash",
        length = 64
    )
    private String emailHash;

    @Column(length = 1024)
    @Convert(converter = PiiStringConverter.class)
    private String name;

    private String lmsUserId;

}
