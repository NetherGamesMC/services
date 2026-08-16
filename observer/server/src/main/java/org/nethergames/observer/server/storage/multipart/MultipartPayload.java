package org.nethergames.observer.server.storage.multipart;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.io.ByteArrayOutputStream;

public class MultipartPayload {
    private final int partNumber;
    private final byte[] bytes;

    public MultipartPayload(int partNumber, ByteArrayOutputStream bytes) {
        this.partNumber = partNumber;
        this.bytes = bytes.toByteArray();

        bytes.reset();
    }

    public void executeUpload(MultipartPackage uploadPackage) {
        var uploadPart = UploadPartRequest.builder()
                .uploadId(uploadPackage.getUploadId())
                .bucket(uploadPackage.getBucketName())
                .key(uploadPackage.getBucketKey())
                .partNumber(partNumber).build();
        var uploaded = uploadPackage.getClient().uploadPart(uploadPart, RequestBody.fromBytes(bytes));

        uploadPackage.getCompletedParts().add(CompletedPart.builder().partNumber(partNumber).eTag(uploaded.eTag()).build());
    }
}
