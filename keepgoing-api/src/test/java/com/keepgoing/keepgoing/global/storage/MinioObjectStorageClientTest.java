package com.keepgoing.keepgoing.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class MinioObjectStorageClientTest {

	private static final String QUARANTINE_BUCKET = "quarantine-bucket";
	private static final String DIRECTORY = "notes/1";
	private static final long FILE_SIZE = 100L;

	@Mock
	private S3Client s3Client;

	@Mock
	private S3Presigner s3Presigner;

	private StorageProperties properties;
	private MinioObjectStorageClient minioClient;

	@BeforeEach
	void setUp() {
		properties = new StorageProperties(
				"http://localhost:9000",
				"access",
				"secret",
				"us-east-1",
				new StorageProperties.BucketNames(QUARANTINE_BUCKET, "secure-bucket")
		);
		minioClient = new MinioObjectStorageClient(s3Client, s3Presigner, properties);
	}

	@Test
	@DisplayName("파일 업로드 시 S3Client의 putObject를 호출하고 저장된 키를 반환한다")
	void uploadSuccessfully() {
		// given
		byte[] content = "test data".getBytes();
		InputStreamSupplier supplier = () -> new ByteArrayInputStream(content);

		// when
		String savedKey = minioClient.upload(supplier, DIRECTORY, content.length);

		// then
		assertThat(savedKey).startsWith(DIRECTORY + "/");

		ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

		PutObjectRequest request = requestCaptor.getValue();
		assertThat(request.bucket()).isEqualTo(QUARANTINE_BUCKET);
		assertThat(request.key()).isEqualTo(savedKey);
	}

	@Test
	@DisplayName("업로드 중 S3Exception 발생 시 ObjectStorageException으로 래핑하여 던진다")
	void uploadThrowsExceptionOnS3Failure() {
		// given
		InputStreamSupplier supplier = () -> new ByteArrayInputStream("data".getBytes());
		given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.willThrow(S3Exception.builder().message("s3 error").build());

		// when & then
		assertThatThrownBy(() -> minioClient.upload(supplier, DIRECTORY, FILE_SIZE))
				.isInstanceOf(ObjectStorageException.class)
				.hasMessageContaining("MinIO 업로드 중 오류 발생")
				.hasCauseInstanceOf(S3Exception.class);
	}

	@Test
	@DisplayName("업로드 중 SdkClientException 발생 시 ObjectStorageException으로 래핑하여 던진다")
	void uploadThrowsExceptionOnSdkClientFailure() {
		// given
		InputStreamSupplier supplier = () -> new ByteArrayInputStream("data".getBytes());
		given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.willThrow(SdkClientException.create("sdk error"));

		// when & then
		assertThatThrownBy(() -> minioClient.upload(supplier, DIRECTORY, FILE_SIZE))
				.isInstanceOf(ObjectStorageException.class)
				.hasMessageContaining("MinIO 업로드 중 오류 발생")
				.hasCauseInstanceOf(SdkClientException.class);
	}

	@Test
	@DisplayName("업로드 스트림 생성 중 IOException 발생 시 ObjectStorageException으로 래핑하여 던진다")
	void uploadThrowsExceptionOnInputStreamFailure() {
		// given
		InputStreamSupplier supplier = () -> {
			throw new IOException("stream open failed");
		};

		// when & then
		assertThatThrownBy(() -> minioClient.upload(supplier, DIRECTORY, FILE_SIZE))
				.isInstanceOf(ObjectStorageException.class)
				.hasMessageContaining("MinIO 업로드 중 오류 발생")
				.hasCauseInstanceOf(IOException.class);
	}

	@Test
	@DisplayName("확장자가 없는 파일명으로 업로드하면 확장자 없이 저장 키를 생성한다")
	void uploadWithoutExtensionCreatesKeyWithoutExtension() {
		// given
		byte[] content = "test data".getBytes();
		InputStreamSupplier supplier = () -> new ByteArrayInputStream(content);

		// when
		String savedKey = minioClient.upload(supplier, DIRECTORY, content.length);

		// then
		assertThat(savedKey).startsWith(DIRECTORY + "/");
		assertThat(savedKey).doesNotContain(".");
	}

	@Test
	@DisplayName("파일명이 null이어도 업로드를 수행하고 확장자 없이 저장 키를 생성한다")
	void uploadWithNullFilenameCreatesKeyWithoutExtension() {
		// given
		byte[] content = "test data".getBytes();
		InputStreamSupplier supplier = () -> new ByteArrayInputStream(content);

		// when
		String savedKey = minioClient.upload(supplier, DIRECTORY, content.length);

		// then
		assertThat(savedKey).startsWith(DIRECTORY + "/");
		assertThat(savedKey).doesNotContain(".");
	}

	@Test
	@DisplayName("Presigned GET URL 생성 시 secure bucket과 path-style key를 포함한다")
	void generatePresignedUrlUsesPathStyleSecureBucketUrl() {
		// given
		String key = "notes/1/safe-image-key";
		try (S3Presigner realPresigner = S3Presigner.builder()
				.region(Region.of("us-east-1"))
				.endpointOverride(URI.create("http://localhost:9000"))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create("access", "secret")
				))
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build())
				.build()) {
			MinioObjectStorageClient client = new MinioObjectStorageClient(s3Client, realPresigner, properties);

			// when
			String presignedUrl = client.generatePresignedUrl("secure-bucket", key, Duration.ofMinutes(5));

			// then
			assertThat(presignedUrl).startsWith("http://localhost:9000/secure-bucket/notes/1/safe-image-key?");
			assertThat(presignedUrl).contains("X-Amz-Expires=300");
			assertThat(presignedUrl).contains("X-Amz-Signature=");
		}
	}

	@Test
	@DisplayName("파일 삭제 시 S3Client의 deleteObject를 호출한다")
	void deleteSuccessfully() {
		// given
		String storageKey = "notes/1/uuid";

		// when
		minioClient.delete(storageKey);

		// then
		ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(s3Client).deleteObject(requestCaptor.capture());

		DeleteObjectRequest request = requestCaptor.getValue();
		assertThat(request.bucket()).isEqualTo(QUARANTINE_BUCKET);
		assertThat(request.key()).isEqualTo(storageKey);
	}

	@Test
	@DisplayName("버킷을 지정해 파일을 삭제하면 지정한 버킷과 키로 S3Client의 deleteObject를 호출한다")
	void deleteSuccessfullyFromSpecifiedBucket() {
		// given
		String bucketName = "secure-bucket";
		String storageKey = "notes/1/safe-image-key";

		// when
		minioClient.delete(bucketName, storageKey);

		// then
		ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
		verify(s3Client).deleteObject(requestCaptor.capture());

		DeleteObjectRequest request = requestCaptor.getValue();
		assertThat(request.bucket()).isEqualTo(bucketName);
		assertThat(request.key()).isEqualTo(storageKey);
	}

	@Test
	@DisplayName("삭제 중 SdkClientException 발생 시 ObjectStorageException으로 래핑하여 던진다")
	void deleteThrowsExceptionOnFailure() {
		// given
		String storageKey = "notes/1/uuid";
		given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
				.willThrow(SdkClientException.create("network error"));

		// when & then
		assertThatThrownBy(() -> minioClient.delete(storageKey))
				.isInstanceOf(ObjectStorageException.class)
				.hasMessageContaining("MinIO 파일 삭제 실패")
				.hasCauseInstanceOf(SdkClientException.class);
	}

	@Test
	@DisplayName("삭제 중 S3Exception 발생 시 ObjectStorageException으로 래핑하여 던진다")
	void deleteThrowsExceptionOnS3Failure() {
		// given
		String storageKey = "notes/1/uuid";
		given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
				.willThrow(S3Exception.builder().message("delete s3 error").build());

		// when & then
		assertThatThrownBy(() -> minioClient.delete(storageKey))
				.isInstanceOf(ObjectStorageException.class)
				.hasMessageContaining("MinIO 파일 삭제 실패")
				.hasCauseInstanceOf(S3Exception.class);
	}

	@Test
	@DisplayName("버킷 지정 삭제 중 S3Exception 발생 시 ObjectStorageException으로 래핑하여 던진다")
	void deleteFromSpecifiedBucketThrowsExceptionOnS3Failure() {
		// given
		String storageKey = "notes/1/safe-image-key";
		given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
				.willThrow(S3Exception.builder().message("delete s3 error").build());

		// when & then
		assertThatThrownBy(() -> minioClient.delete("secure-bucket", storageKey))
				.isInstanceOf(ObjectStorageException.class)
				.hasMessageContaining("MinIO 파일 삭제 실패")
				.hasCauseInstanceOf(S3Exception.class);
	}
}
