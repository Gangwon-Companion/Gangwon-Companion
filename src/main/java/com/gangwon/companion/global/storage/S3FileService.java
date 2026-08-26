package com.gangwon.companion.global.storage;

import com.gangwon.companion.global.config.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileService {
    private static final Duration EXPIRATION = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_EXPIRATION = Duration.ofMinutes(30);
    private final S3Presigner presigner;
    private final AwsS3Properties properties;

    public UploadUrl createUploadUrl(String originalFileName, String contentType) {
        String extension = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot >= 0) extension = originalFileName.substring(dot).toLowerCase();
        String key = "community/" + UUID.randomUUID() + extension;
        PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(properties.getS3().getBucket()).key(key).contentType(contentType).build();
        PutObjectPresignRequest request = PutObjectPresignRequest.builder().signatureDuration(EXPIRATION).putObjectRequest(objectRequest).build();
        return new UploadUrl(key, presigner.presignPutObject(request).url().toString(), (int) EXPIRATION.toSeconds());
    }

    public String createDownloadUrl(String s3Key) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(properties.getS3().getBucket())
                .key(s3Key)
                .build();
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_EXPIRATION)
                .getObjectRequest(objectRequest)
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    public record UploadUrl(String key, String url, int expiresInSeconds) {}
}
