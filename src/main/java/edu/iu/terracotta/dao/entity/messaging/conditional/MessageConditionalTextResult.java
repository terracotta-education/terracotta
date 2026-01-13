package edu.iu.terracotta.dao.entity.messaging.conditional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "terr_messaging_conditional_text_result")
public class MessageConditionalTextResult extends BaseMessageEntity {

    @OneToOne
    @JoinColumn(name = "conditional_text_id")
    private MessageConditionalText conditionalText;

    @Lob
    @Column
    private String html;

}
