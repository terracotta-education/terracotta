package edu.iu.terracotta.connectors.generic.dao.model.lms;

import edu.iu.terracotta.connectors.generic.dao.model.lms.base.BaseLmsFile;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class LmsFile implements BaseLmsFile {

    protected Class<?> type;
    protected String id;
    protected String displayName;
    protected String filename;
    protected long size;
    protected String url;

    @Override
    public LmsFile from() {
        return this;
    }

}
