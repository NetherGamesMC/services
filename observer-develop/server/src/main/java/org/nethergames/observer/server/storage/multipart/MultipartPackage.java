package org.nethergames.observer.server.storage.multipart;

import lombok.Getter;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.Comparator;
import java.util.Vector;

@Getter
public class MultipartPackage {
    private final S3Client client;
    private final String uploadId;
    private final String bucketName;
    private final String bucketKey;
    private final Vector<CompletedPart> completedParts = new Vector<>();

    public MultipartPackage(S3Client client, String uploadId, String bucketName, String bucketKey) {
        this.client = client;
        this.uploadId = uploadId;
        this.bucketName = bucketName;
        this.bucketKey = bucketKey;
    }

    public void completeUpload() {
        completedParts.sort(Comparator.comparingInt(CompletedPart::partNumber));

        var completedUploads = CompletedMultipartUpload.builder()
                .parts(completedParts)
                .build();

        client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .uploadId(uploadId)
                .bucket(bucketName)
                .key(bucketKey)
                .multipartUpload(completedUploads)
                .build());
    }
}
