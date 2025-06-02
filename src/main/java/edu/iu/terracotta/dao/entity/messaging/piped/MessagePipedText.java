package edu.iu.terracotta.dao.entity.messaging.piped;

import java.util.ArrayList;
import java.util.List;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import edu.iu.terracotta.dao.entity.messaging.content.MessageContent;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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
@Table(name = "terr_messaging_piped_text")
public class MessagePipedText extends BaseMessageEntity {

    @Column private String fileName;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "content_id")
    private MessageContent content;

    @Builder.Default
    @OneToMany(
        mappedBy = "pipedText",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessagePipedTextItem> items = new ArrayList<>();

}
