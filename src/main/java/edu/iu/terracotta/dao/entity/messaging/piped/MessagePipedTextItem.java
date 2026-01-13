package edu.iu.terracotta.dao.entity.messaging.piped;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.dao.entity.messaging.BaseMessageEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "terr_messaging_piped_text_item")
public class MessagePipedTextItem extends BaseMessageEntity {

    @Column(name = "item_key")
    private String key;

    @ManyToOne(optional = false)
    @JoinColumn(
        name = "piped_text_id",
        nullable = false
    )
    private MessagePipedText pipedText;

    @Builder.Default
    @OneToMany(
        mappedBy = "item",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<MessagePipedTextItemValue> values = new ArrayList<>();

}
