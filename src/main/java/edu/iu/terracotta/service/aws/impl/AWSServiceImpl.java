package edu.iu.terracotta.service.aws.impl;

import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import edu.iu.terracotta.service.aws.AwsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Component
public class AwsServiceImpl implements AwsService {

    @Value("${aws.region:US_EAST_1}")
    private String region;

    @Value("${aws.enabled:true}")
    private boolean enabled;

    private S3Client amazonS3;

    @PostConstruct
    protected void initializeAmazon() {
        if (!enabled) {
            return;
        }

        this.amazonS3 = S3Client.builder()
            .credentialsProvider(InstanceProfileCredentialsProvider.builder().asyncCredentialUpdateEnabled(false).build())
            .region(Region.of(region))
            .build();
    }

    @Override
    public InputStream readFileFromS3Bucket(String bucketName, String key) {
        return amazonS3.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build());
    }

}
