package edu.iu.terracotta.service.aws.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import edu.iu.terracotta.service.aws.AWSService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;

@Component
public class AWSServiceImpl implements AWSService {

    @Value("${aws.region}")
    private String region;


    private AmazonS3 amazonS3;


    @PostConstruct
    private void initializeAmazon() {
        this.amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new InstanceProfileCredentialsProvider(false))
                .withRegion(Regions.valueOf(region))
                .build();
    }


    @Override
    public InputStream readFileFromS3Bucket(String bucketName, String key) {
        S3Object s3Object = this.amazonS3.getObject(new GetObjectRequest(bucketName, key));
        InputStream inputStream = s3Object.getObjectContent();
        return inputStream;
    }


    @Override
    public String putObject(String bucketName, String fileName, String extention, File file) {
        UUID id = UUID.randomUUID();
        String key = file.getName() + "_" + id.toString() + "." + extention;
        new PutObjectRequest(bucketName, key, file);
        this.amazonS3.putObject(new PutObjectRequest(bucketName, key, file));
        URL url = this.amazonS3.getUrl(bucketName, key);
        return url.toExternalForm();
    }

    @Override
    public String getFileURI(String url) {
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = Instant.now().toEpochMilli();
        expTimeMillis += 1000 * 60 * 60; // Add 1 hour.
        expiration.setTime(expTimeMillis);
        AmazonS3URI amazonS3URI = new AmazonS3URI(url);
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(amazonS3URI.getBucket(), amazonS3URI.getKey());
        generatePresignedUrlRequest.setMethod(HttpMethod.GET);
        generatePresignedUrlRequest.setExpiration(expiration);
        URL uri = this.amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
        return uri.toString();
    }
}
