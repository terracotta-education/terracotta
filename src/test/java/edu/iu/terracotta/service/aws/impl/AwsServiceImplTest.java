package edu.iu.terracotta.service.aws.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Plain Mockito unit test for {@link AwsServiceImpl}. This class has no repository/service
 * constructor dependencies, so it does not need to extend {@code BaseTest}; it is constructed
 * directly and its {@code @Value}-injected fields and internal {@code S3Client} are set via
 * {@link ReflectionTestUtils}.
 */
class AwsServiceImplTest {

    private AwsServiceImpl awsService;
    private S3Client mockS3Client;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        awsService = new AwsServiceImpl();
        mockS3Client = mock(S3Client.class);
    }

    @AfterEach
    void afterEach() {
        // Close any real S3Client instances created by initializeAmazon() to release resources.
        Object amazonS3 = ReflectionTestUtils.getField(awsService, "amazonS3");

        if (amazonS3 instanceof S3Client) {
            ((S3Client) amazonS3).close();
        }
    }

    @Test
    void testReadFileFromS3Bucket() {
        ReflectionTestUtils.setField(awsService, "amazonS3", mockS3Client);

        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> responseInputStream = mock(ResponseInputStream.class);
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        InputStream result = awsService.readFileFromS3Bucket("my-bucket", "my-key");

        assertSame(responseInputStream, result);

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(mockS3Client).getObject(captor.capture());
        GetObjectRequest capturedRequest = captor.getValue();
        assertEquals("my-bucket", capturedRequest.bucket());
        assertEquals("my-key", capturedRequest.key());
    }

    @Test
    void testInitializeAmazonWhenDisabled() {
        ReflectionTestUtils.setField(awsService, "enabled", false);
        ReflectionTestUtils.setField(awsService, "region", "us-east-1");

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(awsService, "initializeAmazon"));

        assertNull(ReflectionTestUtils.getField(awsService, "amazonS3"));
    }

    @Test
    void testInitializeAmazonWhenEnabled() {
        ReflectionTestUtils.setField(awsService, "enabled", true);
        ReflectionTestUtils.setField(awsService, "region", "us-east-1");

        // Building the S3Client via the AWS SDK builder chain does not perform any network I/O;
        // region/credentials resolution for an explicitly-provided region and credentials
        // provider happens lazily on first request, so this is safe to invoke directly.
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(awsService, "initializeAmazon"));

        Object amazonS3 = ReflectionTestUtils.getField(awsService, "amazonS3");
        assertInstanceOf(S3Client.class, amazonS3);
    }

}
