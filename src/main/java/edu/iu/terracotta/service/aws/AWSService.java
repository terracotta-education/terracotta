package edu.iu.terracotta.service.aws;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public interface AWSService {

    public InputStream readFileFromS3Bucket(String bucketName, String key);


    public String putObject(String bucketName, String fileName, String extension, File file);

    public String getFileURI(String url);

    public File downloadFileURI(String url) throws FileNotFoundException;
}
