package edu.iu.terracotta.service.app.impl;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.iu.terracotta.service.app.OutboundUrlService;

@Service
public class OutboundUrlServiceImpl implements OutboundUrlService {

    // local development runs the LMS and test tools on localhost or a private network
    @Value("${terracotta.outbound-url.allow-private-addresses:false}")
    private boolean allowPrivateAddresses;

    @Override
    public void validatePublicUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("URL is required");
        }

        URI uri;

        try {
            uri = new URI(url.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(String.format("URL: [%s] is not valid", url), e);
        }

        if (!Strings.CI.equalsAny(uri.getScheme(), "http", "https")) {
            throw new IllegalArgumentException(String.format("URL: [%s] must use http or https", url));
        }

        if (StringUtils.isBlank(uri.getHost())) {
            throw new IllegalArgumentException(String.format("URL: [%s] has no host", url));
        }

        if (allowPrivateAddresses) {
            return;
        }

        InetAddress[] addresses;

        try {
            addresses = resolve(uri.getHost());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(String.format("URL: [%s] has a host that does not resolve", url), e);
        }

        for (InetAddress address : addresses) {
            if (isInternalAddress(address)) {
                throw new IllegalArgumentException(String.format("URL: [%s] resolves to an internal address", url));
            }
        }
    }

    protected InetAddress[] resolve(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    private boolean isInternalAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();

        if (address instanceof Inet4Address) {
            // 0.0.0.0/8 and the 100.64.0.0/10 shared address space some cloud providers use internally
            return bytes[0] == 0 || (bytes[0] == 100 && (bytes[1] & 0xC0) == 64);
        }

        // IPv6 unique local addresses (fc00::/7)
        return address instanceof Inet6Address && (bytes[0] & 0xFE) == 0xFC;
    }

}
