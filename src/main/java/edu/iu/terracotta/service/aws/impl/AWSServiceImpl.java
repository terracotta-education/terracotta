package edu.iu.terracotta.service.aws.impl;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import edu.iu.terracotta.service.aws.AWSService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Component
public class AWSServiceImpl implements AWSService {

    @Value("${aws.region}")
    private String region;

    private AmazonS3 amazonS3;

    @PostConstruct
    protected void initializeAmazon() {
        this.amazonS3 = AmazonS3ClientBuilder.standard()
            .withCredentials(new InstanceProfileCredentialsProvider(false))
            .withRegion(Regions.valueOf(region))
            .build();
    }

    @Override
    public InputStream readFileFromS3Bucket(String bucketName, String key) {
        S3Object s3Object = this.amazonS3.getObject(new GetObjectRequest(bucketName, key));

        return s3Object.getObjectContent();
    }

}
