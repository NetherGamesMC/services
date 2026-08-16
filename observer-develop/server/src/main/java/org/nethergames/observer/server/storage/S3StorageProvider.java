package org.nethergames.observer.server.storage;

import lombok.Getter;
import org.nethergames.observer.server.exception.UploadFailureException;
import org.nethergames.observer.server.storage.multipart.MultipartPackage;
import org.nethergames.observer.server.storage.multipart.MultipartPayload;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class S3StorageProvider {

    private static final int RETRY_MAX_LIMIT = 4;
    private static final int MULTIPART_UPLOAD_LIMIT = 10 * 1024 * 1024;

    @Getter
    private final S3Client storage;
    @Getter
    private final S3Presigner altStorage;

    private final ThreadPoolExecutor executor;

    private final String bucketName;
    private final String accessKey;
    private final String secretKey;

    public S3StorageProvider(String host, String bucket, String region, String accessKey, String secretKey) {
        executor = new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new SynchronousQueue<>());

        this.bucketName = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;

        storage = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(this::generateCredentials)
                .endpointOverride(URI.create(host)).build();

        altStorage = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(this::generateCredentials)
                .endpointOverride(URI.create(host)).build();
    }

    private AwsCredentials generateCredentials() {
        return new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return accessKey;
            }

            @Override
            public String secretAccessKey() {
                return secretKey;
            }
        };
    }

    public boolean isObjectExists(String bucketLocation) {
        try {
            storage.headObject(HeadObjectRequest.builder().bucket(bucketName).key(bucketLocation).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    public ResponseInputStream<GetObjectResponse> getObjectStream(String location) {
        var object = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(location)
                .build();

        return storage.getObject(object);
    }

    public String getObjectUrl(String bucketLocation) {
        var object = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(bucketLocation)
                .build();

        var request = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(12))
                .getObjectRequest(object)
                .build();

        return altStorage.presignGetObject(request).url().toString();
    }

    public void deleteObject(String bucketLocation) {
        storage.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(bucketLocation).build());
    }

    /**
     * Upload a given input stream into AWS S3 storage server. The punishment uploaded uses multipart upload method,
     * the method allows parts to be split allowing multiple uploads to be done at once.
     *
     * @param bucketLocation The location for the stream to be saved into.
     * @param contentType    MIME content-types data.
     * @param stream         The input stream containing the punishment data.
     */
    public void uploadStreamMultipart(String bucketLocation, String contentType, InputStream stream) {
        var uploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(bucketLocation)
                .contentType(contentType)
                .expires(Instant.now().plus(60, ChronoUnit.MINUTES))
                .build();

        // Try to create a multipart upload request, we will get an upload id
        // if this operation is completed without any exceptions.
        CreateMultipartUploadResponse response;
        try {
            response = storage.createMultipartUpload(uploadRequest);
        } catch (Throwable error) {
            throw new UploadFailureException(error.getMessage(), null);
        }

        var multipartPackage = new MultipartPackage(storage, response.uploadId(), bucketName, bucketLocation);

        try {
            var byteArray = new ByteArrayOutputStream(MULTIPART_UPLOAD_LIMIT);
            var pendingParts = new ArrayList<CompletableFuture<?>>();

            int data, part = 0;
            while ((data = stream.read()) != -1) {
                byteArray.write(data);

                if (byteArray.size() == MULTIPART_UPLOAD_LIMIT) {
                    pendingParts.add(executeUpload(new MultipartPayload(++part, byteArray), multipartPackage));
                }
            }

            if (byteArray.size() > 0) {
                pendingParts.add(executeUpload(new MultipartPayload(++part, byteArray), multipartPackage));
            }

            CompletableFuture.allOf(pendingParts.toArray(new CompletableFuture[0])).join();

            multipartPackage.completeUpload();
        } catch (Throwable error) {
            var abortRequest = AbortMultipartUploadRequest.builder()
                    .uploadId(multipartPackage.getUploadId())
                    .bucket(multipartPackage.getBucketName())
                    .key(multipartPackage.getBucketKey())
                    .build();

            storage.abortMultipartUpload(abortRequest);

            throw new UploadFailureException(error.getMessage(), null);
        }
    }

    private CompletableFuture<Void> executeUpload(MultipartPayload payload, MultipartPackage multipartPackage) {
        return executeUpload0(payload, multipartPackage)
                .thenApply(CompletableFuture::completedFuture)
                .exceptionally(t -> retry(payload, multipartPackage, t, 0))
                .thenCompose(Function.identity());
    }

    private CompletableFuture<Void> executeUpload0(MultipartPayload payload, MultipartPackage multipartPackage) {
        return new CompletableFuture<Void>().completeAsync(() -> {
            payload.executeUpload(multipartPackage);

            return null;
        }, executor);
    }

    private CompletableFuture<Void> retry(MultipartPayload payload, MultipartPackage multipartPackage, Throwable first, int retry) {
        if (retry >= RETRY_MAX_LIMIT) return CompletableFuture.failedFuture(first);
        return executeUpload0(payload, multipartPackage)
                .thenApply(CompletableFuture::completedFuture)
                .exceptionally(t -> {
                    first.addSuppressed(t);
                    return retry(payload, multipartPackage, first, retry + 1);
                })
                .thenCompose(Function.identity());
    }
}
