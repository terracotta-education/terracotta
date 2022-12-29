package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "terr_consent_document")
public class ConsentDocument extends BaseEntity {

    @Id
    @Column(nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long consentDocumentId;

    @Column
    private String title;

    @Column
    private String filePointer;

    @Lob
    @Column
    private String html;

    @OneToOne(mappedBy = "consentDocument")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Experiment experiment;

    @Column
    private String lmsAssignmentId;

    @Column
    private String resourceLinkId;

}
