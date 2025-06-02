package edu.iu.terracotta.connectors.generic.dao.model.lms;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsFolder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsFolder implements BaseLmsFolder {

    protected Class<?> type;
    private String id;
    private String name;
    private String fullName;
    private String filesUrl;

    @Override
    public LmsFolder from() {
        return this;
    }

}
