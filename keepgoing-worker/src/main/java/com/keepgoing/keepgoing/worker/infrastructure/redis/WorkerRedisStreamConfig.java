package com.keepgoing.keepgoing.worker.infrastructure.redis;

import com.keepgoing.keepgoing.worker.infrastructure.redis.adapter.in.RedisImageProcessingRequestListener;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

@Configuration
@EnableConfigurationProperties(WorkerRedisStreamProperties.class)
@RequiredArgsConstructor
@Slf4j
public class WorkerRedisStreamConfig {

	private final WorkerRedisStreamProperties properties;
	private final StringRedisTemplate redisTemplate;

	@Bean
	@ConditionalOnProperty(
			prefix = "image-processing.streams",
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	StreamMessageListenerContainer<String, MapRecord<String, String, String>> requestStreamContainer(
			RedisConnectionFactory connectionFactory,
			RedisImageProcessingRequestListener listener
	) {
		createRequestConsumerGroupIfAbsent();

		var options = StreamMessageListenerContainerOptions.builder()
				.pollTimeout(Duration.ofSeconds(1))
				.build();

		var container = StreamMessageListenerContainer.create(connectionFactory, options);

		container.receive(
				Consumer.from(properties.requestGroup(), properties.requestConsumer()),
				StreamOffset.create(properties.request(), ReadOffset.lastConsumed()),
				listener::handle
		);

		container.start();
		return container;
	}

	private void createRequestConsumerGroupIfAbsent() {
		try {
			redisTemplate.opsForStream()
					.createGroup(properties.request(), ReadOffset.from("0-0"), properties.requestGroup());
		} catch (RedisSystemException e) {
			if (isBusyGroup(e)) {
				log.info("소비자 그룹이 이미 존재합니다: {}", properties.requestGroup());
			} else {
				throw e;
			}
		}
	}

	private boolean isBusyGroup(RedisSystemException e) {
		Throwable current = e;

		while (current != null) {
			String message = current.getMessage();
			if (message != null && message.contains("BUSYGROUP")) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
