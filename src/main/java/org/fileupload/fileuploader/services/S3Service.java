package org.fileupload.fileuploader.services;


import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;


    // we have to update this file
    public S3Service(String accessKey, String secretKey, String bucketName, String region) {
        this.bucketName = bucketName;
        this.region = region;

        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    public void uploadFile(Path filePath, String s3Key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        s3Client.putObject(request, filePath);
    }

    public void shutdown() {
        s3Client.close();
    }

}
