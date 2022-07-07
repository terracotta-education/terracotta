package edu.iu.terracotta.service.aws.impl;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
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

    @Value("${aws.accessKey}")
    private String accessKey;


    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;


    private BasicAWSCredentials awsCredentials;

    private AmazonS3 amazonS3;


    @PostConstruct
    private void initializeAmazon() {
        this.awsCredentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
        this.amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.valueOf(region))
                .build();
    }


    @Override
    public InputStream readFileFromS3Bucket(String key, String bucketName) {
        S3Object s3Object = this.amazonS3.getObject(new GetObjectRequest(bucketName, key));
        InputStream inputStream = s3Object.getObjectContent();
        return inputStream;
    }
}
