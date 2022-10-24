package edu.iu.terracotta.service.aws;

import java.io.File;
import java.io.InputStream;

public interface AWSService {

    public InputStream readFileFromS3Bucket(String bucketName, String key);


    public String putObject(String bucketName,  File file);
}
