package com.keepgoing.keepgoing.worker.infrastructure.adapter.out.storage;

import com.keepgoing.keepgoing.worker.application.port.out.ImageStorageException;
import com.keepgoing.keepgoing.worker.application.port.out.ImageStoragePort;
import com.keepgoing.keepgoing.worker.application.port.out.QuarantineObjectNotFoundException;
import com.keepgoing.keepgoing.worker.infrastructure.config.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class MinioImageStorageAdapter implements ImageStoragePort {

	private final S3Client s3Client;
	private final StorageProperties properties;

	@Override
	public byte[] readQuarantineObject(String storageKey) {
		try {
			GetObjectRequest request = GetObjectRequest.builder()
					.bucket(properties.bucketNames().quarantine())
					.key(storageKey)
					.build();

			ResponseBytes<GetObjectResponse> response
					= s3Client.getObjectAsBytes(request);
			return response.asByteArray();
		} catch (NoSuchKeyException e) {
			throw new QuarantineObjectNotFoundException(storageKey, e);
		} catch (AwsServiceException | SdkClientException e) {
			throw new ImageStorageException(
					"quarantine object 읽기 실패: " + storageKey, e
			);
		}
	}

	@Override
	public void deleteQuarantineObject(String storageKey) {
		try {
			DeleteObjectRequest request = DeleteObjectRequest.builder()
					.bucket(properties.bucketNames().quarantine())
					.key(storageKey)
					.build();

			s3Client.deleteObject(request);
		} catch (AwsServiceException | SdkClientException e) {
			throw new ImageStorageException(
					"quarantine object 삭제 실패: " + storageKey, e
			);
		}
	}

	@Override
	public void putSecureObject(String storageKey, byte[] bytes, String contentType) {
		try {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(properties.bucketNames().secure())
					.key(storageKey)
					.contentType(contentType)
					.contentLength((long) bytes.length)
					.build();

			s3Client.putObject(request, RequestBody.fromBytes(bytes));
		} catch (AwsServiceException | SdkClientException e) {
			throw new ImageStorageException(
					"secure object 저장 실패: " + storageKey, e
			);
		}
	}
}
