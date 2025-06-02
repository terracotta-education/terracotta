package edu.iu.terracotta.connectors.generic.dao.model.lms.base;

import edu.iu.terracotta.connectors.generic.dao.model.lms.LmsConversation;

public interface BaseLmsConversation extends LmsEntity<LmsConversation> {

    String getId();
    LmsConversation from();

}
