package edu.iu.terracotta.service.aws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public interface AWSService {

    InputStream readFileFromS3Bucket(String bucketName, String key);

    String putObject(String bucketName, String fileName, String extension, File file);

    File downloadFileURI(String url) throws FileNotFoundException;

}
