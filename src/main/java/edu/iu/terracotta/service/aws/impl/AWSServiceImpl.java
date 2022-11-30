package edu.iu.terracotta.service.aws.impl;


//import com.amazonaws.HttpMethod;
//import com.amazonaws.auth.InstanceProfileCredentialsProvider;
//import com.amazonaws.regions.Regions;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//import com.amazonaws.services.s3.AmazonS3URI;
//import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
//import com.amazonaws.services.s3.model.GetObjectRequest;
//import com.amazonaws.services.s3.model.PutObjectRequest;
//import com.amazonaws.services.s3.model.S3Object;

import com.amazonaws.services.s3.AmazonS3URI;
import edu.iu.terracotta.service.aws.AWSService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

@Component
public class AWSServiceImpl implements AWSService {

    @Value("${aws.region}")
    private String region;


    private S3Client s3Client;


    @PostConstruct
    private void initializeAmazon() {
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
    }


    @Override
    public InputStream readFileFromS3Bucket(String bucketName, String key) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        InputStream inputStream = s3Client.getObject(getObjectRequest);
        return inputStream;
    }


    @Override
    public String putObject(String bucketName, String fileName, String extention, File file) {
        UUID id = UUID.randomUUID();
        String key = file.getName() + "_" + id.toString() + "." + extention;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(objectRequest, RequestBody.fromFile(file));
        GetUrlRequest request = GetUrlRequest.builder().bucket(bucketName).key(key).build();
        return s3Client.utilities().getUrl(request).toExternalForm();
    }


    @Override
    public File downloadFileURI(String url) throws FileNotFoundException {
        AmazonS3URI amazonS3URI = new AmazonS3URI(url);
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(amazonS3URI.getBucket())
                .key(amazonS3URI.getKey())
                .build();
        String path = "/tmp/" + UUID.randomUUID()+"_"+amazonS3URI.getKey();
        File temp = new File(path);
        s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(temp));
        return temp;
    }
}
