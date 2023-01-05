package edu.iu.terracotta.model.app;

import edu.iu.terracotta.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "terr_answer_file_submission")
public class AnswerFileSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long answerFileSubmissionId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "quest_sub_quest_sub_id", nullable = false)
    private QuestionSubmission questionSubmission;

    @Lob
    @Column
    private byte[] fileContent;

    @Column
    private String fileName;

    @Column
    private  String mimeType;

    @Column
    private String fileUri;

}
