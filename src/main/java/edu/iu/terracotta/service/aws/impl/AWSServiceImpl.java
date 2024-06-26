package edu.iu.terracotta.service.aws.impl;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import edu.iu.terracotta.service.aws.AWSService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Component
public class AWSServiceImpl implements AWSService {

    @Value("${aws.region:US_EAST_1}")
    private String region;

    @Value("${aws.enabled:true}")
    private boolean enabled;

    private AmazonS3 amazonS3;

    @PostConstruct
    protected void initializeAmazon() {
        if (!enabled) {
            return;
        }

        this.amazonS3 = AmazonS3ClientBuilder.standard()
            .withCredentials(new InstanceProfileCredentialsProvider(false))
            .withRegion(Regions.valueOf(region))
            .build();
    }

    @Override
    public InputStream readFileFromS3Bucket(String bucketName, String key) {
        return amazonS3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent();
    }

}
