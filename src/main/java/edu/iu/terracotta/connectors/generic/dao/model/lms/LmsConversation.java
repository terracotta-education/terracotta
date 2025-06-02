package edu.iu.terracotta.connectors.generic.dao.model.lms;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsConversation;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsConversation implements BaseLmsConversation {

    protected Class<?> type;
    protected String id;

    @Override
    public LmsConversation from() {
        return this;
    }

}
