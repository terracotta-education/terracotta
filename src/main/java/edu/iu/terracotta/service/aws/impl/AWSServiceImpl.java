package edu.iu.terracotta.service.aws.impl;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import edu.iu.terracotta.service.aws.AWSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
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
    public String putObject(String bucketName, File file) {
        UUID id = UUID.randomUUID();
        String key = file.getName()+"_"+id.toString();
        new PutObjectRequest(bucketName, key,file);
        this.amazonS3.putObject(new PutObjectRequest(bucketName, key,file));
        URL url =  this.amazonS3.getUrl(bucketName,key);
        return url.toExternalForm();
    }
}
