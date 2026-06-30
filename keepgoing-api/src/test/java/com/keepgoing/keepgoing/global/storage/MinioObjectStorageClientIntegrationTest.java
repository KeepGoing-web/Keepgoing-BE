package com.keepgoing.keepgoing.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Testcontainers
class MinioObjectStorageClientIntegrationTest {

	private static final String ACCESS_KEY = "test-access-key";
	private static final String SECRET_KEY = "test-secret-key";
	private static final String REGION = "us-east-1";
	private static final String QUARANTINE_BUCKET = "quarantine-bucket";
	private static final String SECURE_BUCKET = "secure-bucket";
	private static final String DIRECTORY = "notes/1";

	@Container
	static final GenericContainer<?> minio =
			new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2025-09-07T16-13-09Z"))
					.withEnv("MINIO_ROOT_USER", ACCESS_KEY)
					.withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
					.withCommand("server", "/data", "--console-address", ":9001")
					.withExposedPorts(9000)
					.waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

	private S3Client s3Client;
	private S3Presigner s3Presigner;
	private MinioObjectStorageClient minioClient;

	@BeforeEach
	void setUp() {
		String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
		StorageProperties properties = new StorageProperties(
				endpoint,
				ACCESS_KEY,
				SECRET_KEY,
				REGION,
				new StorageProperties.BucketNames(QUARANTINE_BUCKET, SECURE_BUCKET)
		);
		s3Client = S3Client.builder()
				.endpointOverride(URI.create(endpoint))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
				))
				.region(Region.of(REGION))
				.forcePathStyle(true)
				.build();
		s3Presigner = S3Presigner.builder()
				.endpointOverride(URI.create(endpoint))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)
				))
				.region(Region.of(REGION))
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build())
				.build();
		minioClient = new MinioObjectStorageClient(s3Client, s3Presigner, properties);

		createBucketIfAbsent(QUARANTINE_BUCKET);
		createBucketIfAbsent(SECURE_BUCKET);
	}

	@AfterEach
	void tearDown() {
		if (s3Client != null) {
			s3Client.close();
		}
		if (s3Presigner != null) {
			s3Presigner.close();
		}
	}

	@Test
	@DisplayName("실제 MinIO 업로드 시 quarantine 버킷에만 객체를 저장한다")
	void uploadStoresObjectOnlyInQuarantineBucket() {
		// given
		byte[] content = "image-content".getBytes(StandardCharsets.UTF_8);
		InputStreamSupplier supplier = () -> new ByteArrayInputStream(content);

		// when
		String savedKey = minioClient.upload(supplier, DIRECTORY, content.length);

		// then
		assertThat(savedKey).startsWith(DIRECTORY + "/");
		String objectName = savedKey.substring((DIRECTORY + "/").length());
		assertThatCode(() -> UUID.fromString(objectName))
				.doesNotThrowAnyException();

		ResponseBytes<GetObjectResponse> uploaded = s3Client.getObjectAsBytes(GetObjectRequest.builder()
				.bucket(QUARANTINE_BUCKET)
				.key(savedKey)
				.build());
		assertThat(uploaded.asByteArray()).isEqualTo(content);

		assertThatObjectDoesNotExist(SECURE_BUCKET, savedKey);
	}

	@Test
	@DisplayName("실제 MinIO 삭제 시 quarantine 버킷의 객체를 삭제한다")
	void deleteRemovesObjectFromQuarantineBucket() {
		// given
		byte[] content = "delete-target".getBytes(StandardCharsets.UTF_8);
		String savedKey = minioClient.upload(
				() -> new ByteArrayInputStream(content),
				DIRECTORY,
				content.length
		);

		// when
		minioClient.delete(savedKey);

		// then
		assertThatObjectDoesNotExist(QUARANTINE_BUCKET, savedKey);
	}

	@Test
	@DisplayName("실제 MinIO secure 객체에 대한 path-style Presigned URL을 생성하고 조회할 수 있다")
	void generatePresignedUrlForSecureObject() throws Exception {
		// given
		String secureKey = "notes/1/safe-image-key";
		byte[] content = "safe-image-content".getBytes(StandardCharsets.UTF_8);
		s3Client.putObject(PutObjectRequest.builder()
				.bucket(SECURE_BUCKET)
				.key(secureKey)
				.contentType("image/png")
				.build(), RequestBody.fromBytes(content));

		// when
		String presignedUrl = minioClient.generatePresignedUrl(SECURE_BUCKET, secureKey, Duration.ofMinutes(5));
		HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
				HttpRequest.newBuilder(URI.create(presignedUrl)).GET().build(),
				HttpResponse.BodyHandlers.ofByteArray()
		);

		// then
		URI uri = URI.create(presignedUrl);
		assertThat(uri.getPath()).isEqualTo("/" + SECURE_BUCKET + "/" + secureKey);
		assertThat(uri.getQuery()).contains("X-Amz-Signature=");
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).isEqualTo(content);
	}

	@Test
	@DisplayName("버킷이 이미 있어도 테스트 초기화는 실패하지 않는다")
	void setupDoesNotFailWhenBucketsAlreadyExist() {
		// when & then
		createBucketIfAbsent(QUARANTINE_BUCKET);
		createBucketIfAbsent(SECURE_BUCKET);
	}

	private void createBucketIfAbsent(String bucketName) {
		try {
			s3Client.createBucket(CreateBucketRequest.builder()
					.bucket(bucketName)
					.build());
		} catch (S3Exception e) {
			if (!isBucketAlreadyExists(e)) {
				throw e;
			}
		}
	}

	private boolean isBucketAlreadyExists(S3Exception e) {
		String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
		return e.statusCode() == 409
				|| "BucketAlreadyOwnedByYou".equals(errorCode)
				|| "BucketAlreadyExists".equals(errorCode);
	}

	private void assertThatObjectDoesNotExist(String bucketName, String key) {
		assertThatThrownBy(() -> s3Client.headObject(HeadObjectRequest.builder()
				.bucket(bucketName)
				.key(key)
				.build()))
				.isInstanceOf(S3Exception.class)
				.extracting("statusCode")
				.isEqualTo(404);
	}
}
