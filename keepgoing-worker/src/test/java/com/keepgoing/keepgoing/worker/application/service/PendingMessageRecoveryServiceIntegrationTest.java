package com.keepgoing.keepgoing.worker.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.keepgoing.keepgoing.common.image.event.ImageProcessingRequestedEvent;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingCommand;
import com.keepgoing.keepgoing.worker.application.port.in.ImageProcessingUseCase;
import com.keepgoing.keepgoing.worker.application.port.out.PendingMessageClaimPort;
import com.keepgoing.keepgoing.worker.infrastructure.adapter.in.schedule.PendingMessageRecoveryScheduler;
import com.keepgoing.keepgoing.worker.infrastructure.config.redis.WorkerRedisStreamProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
		"image-processing.streams.pending-idle-timeout=0",
		"image-processing.streams.pending-batch-size=1",
		"image-processing.streams.pending-interval=9999999"
})
@Testcontainers
class PendingMessageRecoveryServiceIntegrationTest {

	private static final Instant REQUESTED_AT = Instant.parse("2026-05-15T00:00:00Z");
	private static final String STORAGE_KEY = "notes/10/generated-image-key";
	private static final String CONTENT_TYPE = "image/png";
	private static final long FILE_SIZE = 1024L;

	@MockitoBean
	ImageProcessingUseCase imageProcessing;

	@MockitoBean
	PendingMessageRecoveryScheduler recoveryScheduler;

	@Autowired
	StringRedisTemplate redisTemplate;

	@Autowired
	WorkerRedisStreamProperties properties;

	@Autowired
	PendingMessageRecoveryService recoveryService;

	@Autowired
	PendingMessageClaimPort pendingMessageClaim;

	@Container
	@SuppressWarnings("resource")
	static GenericContainer<?> redis =
			new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
					.withExposedPorts(6379);

	@DynamicPropertySource
	static void registerRedisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@BeforeEach
	void setUp() {
		redisTemplate.opsForStream()
				.createGroup(properties.request(), ReadOffset.from("0-0"), properties.requestGroup());
	}

	@AfterEach
	void tearDown() {
		redisTemplate.delete(properties.request());
		redisTemplate.delete(properties.dlq());
	}

	@Test
	@DisplayName("PEL에 idle 메시지가 있으면 claim하고 process를 호출한 뒤 ACK한다")
	void pendingMessage_claimAndProcessAndAck() {
		// given
		ImageProcessingRequestedEvent event = requestedEvent(0);
		putMessageIntoPEL(event);

		// when
		recoveryService.recoverPendingMessages();

		// then
		ArgumentCaptor<ImageProcessingCommand> commandCaptor = ArgumentCaptor.forClass(ImageProcessingCommand.class);
		then(imageProcessing).should().process(commandCaptor.capture());
		assertThat(commandCaptor.getValue()).isEqualTo(commandFrom(event));
		assertThat(pendingCount()).isZero();
	}

	@Test
	@DisplayName("idle 기준에 미달한 PEL 메시지는 claim하지 않는다")
	void pendingMessage_underIdleTimeout_doesNotClaim() {
		// given
		putMessageIntoPEL(requestedEvent(0));

		// when
		var claimed = pendingMessageClaim.claimIdleMessages(Duration.ofMillis(60_000), properties.pendingBatchSize());

		// then
		assertThat(claimed).isEmpty();
		assertThat(pendingCount()).isEqualTo(1);
		then(imageProcessing).shouldHaveNoInteractions();
	}

	@Test
	@DisplayName("pendingBatchSize만큼만 PEL 메시지를 claim하고 처리한다")
	void pendingMessages_respectsBatchSize() {
		// given
		putMessageIntoPEL(requestedEvent(0));
		putMessageIntoPEL(requestedEvent(0));

		// when
		recoveryService.recoverPendingMessages();

		// then
		then(imageProcessing).should().process(any());
		then(imageProcessing).shouldHaveNoMoreInteractions();
		assertThat(pendingCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("process 실패 시 원본을 ACK하고 전체 payload를 유지한 retry 메시지를 추가한다")
	void processFailure_ackAndScheduleRetry() {
		// given
		willThrow(new RuntimeException("processing failed"))
				.given(imageProcessing).process(any());
		ImageProcessingRequestedEvent event = requestedEvent(0);
		putMessageIntoPEL(event);

		// when
		recoveryService.recoverPendingMessages();

		// then
		assertThat(pendingCount()).isZero();
		List<MapRecord<String, Object, Object>> records = readStream(properties.request());
		assertThat(records).hasSize(2);
		assertThat(records.get(1).getValue())
				.containsExactlyInAnyOrderEntriesOf(messageBody(event, 1));
	}

	@Test
	@DisplayName("retryCount가 maxRetries 이상이면 원본 payload를 유지해 DLQ로 이동한다")
	void retryExhausted_sendToDlq() {
		// given
		ImageProcessingRequestedEvent event = requestedEvent(properties.maxRetries());
		putMessageIntoPEL(event);

		// when
		recoveryService.recoverPendingMessages();

		// then
		then(imageProcessing).shouldHaveNoInteractions();
		assertThat(pendingCount()).isZero();
		List<MapRecord<String, Object, Object>> dlqRecords = readStream(properties.dlq());
		assertThat(dlqRecords).hasSize(1);
		assertThat(dlqRecords.getFirst().getValue())
				.containsExactlyInAnyOrderEntriesOf(event.toMap());
	}

	@Test
	@DisplayName("claim할 메시지가 없으면 process를 호출하지 않는다")
	void noPendingMessages_doesNothing() {
		// when
		recoveryService.recoverPendingMessages();

		// then
		then(imageProcessing).shouldHaveNoInteractions();
		assertThat(pendingCount()).isZero();
	}

	@SuppressWarnings("unchecked")
	private void putMessageIntoPEL(ImageProcessingRequestedEvent event) {
		redisTemplate.opsForStream()
				.add(MapRecord.create(properties.request(), event.toMap()));

		var records = redisTemplate.opsForStream().read(
				Consumer.from(properties.requestGroup(), properties.requestConsumer()),
				StreamOffset.create(properties.request(), ReadOffset.lastConsumed()));

		assertThat(records).isNotEmpty();
	}

	@SuppressWarnings("unchecked")
	private List<MapRecord<String, Object, Object>> readStream(String stream) {
		List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
				.read(StreamOffset.create(stream, ReadOffset.from("0-0")));
		return records == null ? List.of() : records;
	}

	private long pendingCount() {
		return redisTemplate.opsForStream()
				.pending(properties.request(), properties.requestGroup())
				.getTotalPendingMessages();
	}

	private static ImageProcessingRequestedEvent requestedEvent(int retryCount) {
		return new ImageProcessingRequestedEvent(
				UUID.randomUUID(), STORAGE_KEY, CONTENT_TYPE, FILE_SIZE, REQUESTED_AT, retryCount
		);
	}

	private static ImageProcessingCommand commandFrom(ImageProcessingRequestedEvent event) {
		return new ImageProcessingCommand(
				event.publicId(),
				event.storageKey(),
				event.contentType(),
				event.fileSize(),
				event.requestedAt(),
				event.retryCount()
		);
	}

	private static Map<String, String> messageBody(ImageProcessingRequestedEvent event, int retryCount) {
		return new ImageProcessingRequestedEvent(
				event.publicId(),
				event.storageKey(),
				event.contentType(),
				event.fileSize(),
				event.requestedAt(),
				retryCount
		).toMap();
	}
}
