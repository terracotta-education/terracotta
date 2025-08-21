package edu.iu.terracotta.connectors.generic.dao.model.lms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsConversation;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LmsConversation implements BaseLmsConversation {

    protected Class<?> type;
    protected String id;

    @Override
    public LmsConversation from() {
        return this;
    }

}
