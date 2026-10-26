package edu.iu.terracotta.service.app;

public interface OutboundUrlService {

    /**
     * Checks a URL that a user supplied before the server requests it. It must be http or https
     * and its host must not resolve to a loopback, private, link-local or other internal address.
     *
     * @throws IllegalArgumentException if the URL is not allowed
     */
    void validatePublicUrl(String url);

}
