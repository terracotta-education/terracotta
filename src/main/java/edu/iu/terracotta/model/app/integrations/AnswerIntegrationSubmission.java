package edu.iu.terracotta.model.app.integrations;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import edu.iu.terracotta.model.app.QuestionSubmission;
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
@Table(name = "terr_answer_integration_submission")
public class AnswerIntegrationSubmission extends BaseIntegrationEntity {

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "quest_sub_quest_sub_id",
        nullable = false
    )
    private QuestionSubmission questionSubmission;

}
