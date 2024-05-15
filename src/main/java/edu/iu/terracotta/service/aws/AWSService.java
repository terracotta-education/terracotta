package edu.iu.terracotta.service.aws;

import java.io.InputStream;

public interface AWSService {

    InputStream readFileFromS3Bucket(String bucketName, String key);

}
