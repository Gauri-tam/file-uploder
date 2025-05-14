package org.fileupload.fileuploader.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AWSConfig {

    private String accessKey;
    private String secretKey;
    private String bucketName;
    private String region;

    public AWSConfig(String configPath) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            props.load(fis);
            this.accessKey = props.getProperty("aws.accessKey");
            this.secretKey = props.getProperty("aws.secretKey");
            this.bucketName = props.getProperty("aws.bucketName");
            this.region = props.getProperty("aws.region");
        }
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
