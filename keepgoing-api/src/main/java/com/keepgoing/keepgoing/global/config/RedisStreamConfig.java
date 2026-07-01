package com.keepgoing.keepgoing.global.config;

import com.keepgoing.keepgoing.global.redis.RedisStreamProperties;
import com.keepgoing.keepgoing.note.event.NoteImageProcessingResultListener;
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
@EnableConfigurationProperties(RedisStreamProperties.class)
@RequiredArgsConstructor
@Slf4j
public class RedisStreamConfig {

	private final RedisStreamProperties properties;
	private final StringRedisTemplate redisTemplate;

	@Bean
	@ConditionalOnProperty(
			prefix = "image-processing.streams",
			name = "enabled",
			havingValue = "true",
			matchIfMissing = true
	)
	StreamMessageListenerContainer<String, MapRecord<String, String, String>> resultStreamContainer(
			RedisConnectionFactory connectionFactory,
			NoteImageProcessingResultListener listener
	) {
		createResultConsumerGroupIfAbsent();

		var options = StreamMessageListenerContainerOptions.builder()
				.pollTimeout(Duration.ofSeconds(1))
				.build();

		var container = StreamMessageListenerContainer.create(connectionFactory, options);

		container.receive(
				Consumer.from(properties.resultGroup(), properties.resultConsumer()),
				StreamOffset.create(properties.result(), ReadOffset.lastConsumed()),
				listener::handle
		);

		container.start();
		return container;
	}

	private void createResultConsumerGroupIfAbsent() {
		try {
			redisTemplate.opsForStream()
					.createGroup(properties.result(), ReadOffset.from("0-0"), properties.resultGroup());
		} catch (RedisSystemException e) {
			if (isBusyGroup(e)) {
				log.info("소비자 그룹이 이미 존재합니다: {}", properties.resultGroup());
			} else {
				throw e;
			}
		}
	}

	private boolean isBusyGroup(RedisSystemException e) {
		Throwable candidate = e;
		while (candidate != null) {
			if (candidate.getMessage() != null && candidate.getMessage().contains("BUSYGROUP")) {
				return true;
			}
			candidate = candidate.getCause();
		}
		return false;
	}
}
