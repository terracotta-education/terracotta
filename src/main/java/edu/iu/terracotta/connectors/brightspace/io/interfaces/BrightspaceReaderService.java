package edu.iu.terracotta.connectors.brightspace.io.interfaces;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
public interface BrightspaceReaderService<T, R extends BrightspaceReaderService> {

    /*
     * Perform an operation with a callback. This is used to
     * to perform operations on paginated calls before the final
     * response is complete.
     */
    R withCallback(Consumer<List<T>> responseConsumer);

}
