package edu.iu.terracotta.service.app.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

public class OutboundUrlServiceImplTest {

    private OutboundUrlServiceImpl outboundUrlService;

    @BeforeEach
    public void beforeEach() {
        // hostnames resolve to fixed addresses so the tests never depend on real DNS
        outboundUrlService = new OutboundUrlServiceImpl() {
            @Override
            protected InetAddress[] resolve(String host) throws UnknownHostException {
                return switch (host) {
                    case "survey.example.com" -> new InetAddress[] { InetAddress.getByAddress(host, new byte[] { 93, (byte) 184, (byte) 215, 14 }) };
                    case "internal.example.com" -> new InetAddress[] {
                        InetAddress.getByAddress(host, new byte[] { 93, (byte) 184, (byte) 215, 14 }),
                        InetAddress.getByAddress(host, new byte[] { 10, 1, 2, 3 })
                    };
                    case "missing.example.com" -> throw new UnknownHostException(host);
                    default -> super.resolve(host);
                };
            }
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://survey.example.com/s/123?x=1",
        "http://survey.example.com",
        "HTTPS://survey.example.com/path",
        "https://93.184.215.14/page",
        "https://[2606:2800:220:1:248:1893:25c8:1946]/"
    })
    void testValidatePublicUrlAllowsPublicAddresses(String url) {
        assertDoesNotThrow(() -> outboundUrlService.validatePublicUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://127.0.0.1:8080/actuator",
        "http://localhost/admin",
        "http://0.0.0.0/",
        "http://10.0.0.5/",
        "http://172.16.0.1/",
        "http://192.168.1.1/",
        "http://169.254.169.254/latest/meta-data/",
        "http://100.100.100.200/latest/meta-data/",
        "http://[::1]/",
        "http://[fe80::1]/",
        "http://[fd00:ec2::254]/latest/meta-data/",
        "http://[::ffff:127.0.0.1]/",
        "http://224.0.0.1/",
        "https://internal.example.com/"
    })
    void testValidatePublicUrlRejectsInternalAddresses(String url) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> outboundUrlService.validatePublicUrl(url));

        assertTrue(exception.getMessage().contains("internal address"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "file:///etc/passwd",
        "ftp://survey.example.com/file",
        "gopher://survey.example.com:70/",
        "jar:file:///tmp/x.jar!/",
        "//survey.example.com/no-scheme"
    })
    void testValidatePublicUrlRejectsOtherSchemes(String url) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> outboundUrlService.validatePublicUrl(url));

        assertTrue(exception.getMessage().contains("http or https"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "http://[invalid", "http:///path-only", "https://missing.example.com/" })
    void testValidatePublicUrlRejectsMalformedUrls(String url) {
        assertThrows(IllegalArgumentException.class, () -> outboundUrlService.validatePublicUrl(url));
    }

    @Test
    void testValidatePublicUrlRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> outboundUrlService.validatePublicUrl(null));
    }

    @Test
    void testValidatePublicUrlAllowsPrivateAddressesWhenConfigured() {
        ReflectionTestUtils.setField(outboundUrlService, "allowPrivateAddresses", true);

        assertDoesNotThrow(() -> outboundUrlService.validatePublicUrl("http://localhost:8080/openid-configuration"));
        assertThrows(IllegalArgumentException.class, () -> outboundUrlService.validatePublicUrl("file:///etc/passwd"));
    }

}
