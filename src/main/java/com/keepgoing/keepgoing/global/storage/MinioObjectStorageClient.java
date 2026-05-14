package com.keepgoing.keepgoing.global.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class MinioObjectStorageClient implements ObjectStorageClient {

	private final S3Client s3Client;
	private final StorageProperties properties;

	@Override
	public String upload(
			InputStreamSupplier inputStreamSupplier,
			String directory,
			String originalFilename,
			long fileSize
	) {
		String extension = extractExtension(originalFilename);
		String savedKey = directory + "/" + UUID.randomUUID() + extension;

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

	private String extractExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}
		return filename.substring(filename.lastIndexOf("."));
	}
}
