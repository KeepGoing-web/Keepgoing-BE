package com.keepgoing.keepgoing.global.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
public class MinioObjectStorageClient implements ObjectStorageClient {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final StorageProperties properties;

	@Override
	public String upload(
			InputStreamSupplier inputStreamSupplier,
			String directory,
			long fileSize
	) {
		String savedKey = directory + "/" + UUID.randomUUID();

		try (InputStream inputStream = inputStreamSupplier.get()) {
			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
					.bucket(properties.bucketNames().quarantine())
					.key(savedKey)
					.build();
			s3Client.putObject(putObjectRequest,
					RequestBody.fromInputStream(inputStream, fileSize));

			return savedKey;
		} catch (IOException | S3Exception | SdkClientException e) {
			throw new ObjectStorageException("MinIO 업로드 중 오류 발생: " + savedKey, e);
		}
	}

	@Override
	public void delete(String storageKey) {
		try {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
					.bucket(properties.bucketNames().quarantine())
					.key(storageKey)
					.build();

			s3Client.deleteObject(deleteObjectRequest);
		} catch (S3Exception | SdkClientException e) {
			throw new ObjectStorageException("MinIO 파일 삭제 실패: " + storageKey, e);
		}
	}

	@Override
	public void delete(String bucketName, String storageKey) {
		try {
			DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(storageKey)
					.build();

			s3Client.deleteObject(deleteObjectRequest);
		} catch (S3Exception | SdkClientException e) {
			throw new ObjectStorageException("MinIO 파일 삭제 실패: " + storageKey, e);
		}
	}

	@Override
	public String generatePresignedUrl(String bucketName, String key, Duration duration) {
		try {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();

			PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(r -> r
					.signatureDuration(duration)
					.getObjectRequest(getObjectRequest));

			return presignedRequest.url().toString();
		} catch (S3Exception | SdkClientException e) {
			throw new ObjectStorageException("Presigned URL 생성 실패: " + key, e);
		}
	}
}
