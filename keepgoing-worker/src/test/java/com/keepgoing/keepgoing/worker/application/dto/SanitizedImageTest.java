package com.keepgoing.keepgoing.worker.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SanitizedImageTest {

	@Test
	@DisplayName("유효한 재인코딩 결과이면 생성된다")
	void test() {
		// given
		byte[] bytes = new byte[]{1, 2, 3};
		String contentType = "image/png";
		long fileSize = bytes.length;

		// when
		SanitizedImage sanitizedImage = new SanitizedImage(bytes, contentType, fileSize);

		// then
		assertThat(sanitizedImage.bytes()).isEqualTo(bytes);
		assertThat(sanitizedImage.contentType()).isEqualTo(contentType);
		assertThat(sanitizedImage.fileSize()).isEqualTo(fileSize);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidArguments")
	@DisplayName("재인코딩 결과가 유효하지 않으면 생성에 실패한다")
	void throwsWhenArgumentsAreInvalid(
			String name,
			byte[] bytes,
			String contentType,
			long fileSize,
			String expectedMessage
	) {
		assertThatThrownBy(() -> new SanitizedImage(bytes, contentType, fileSize))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage(expectedMessage);
	}

	static Stream<Arguments> invalidArguments() {
		return Stream.of(
				Arguments.of(
						"bytes가 null",
						null,
						"image/png",
						1L,
						"sanitized image bytes must not be empty"
				),
				Arguments.of(
						"bytes가 empty",
						new byte[]{},
						"image/png",
						1L,
						"sanitized image bytes must not be empty"
				),
				Arguments.of(
						"contentType이 null",
						new byte[]{1},
						null,
						1L,
						"sanitized image contentType must not be blank"
				),
				Arguments.of(
						"contentType이 blank",
						new byte[]{1},
						"   ",
						1L,
						"sanitized image contentType must not be blank"
				),
				Arguments.of(
						"fileSize가 0",
						new byte[]{1},
						"image/png",
						0L,
						"sanitized image fileSize must be positive"
				),
				Arguments.of(
						"fileSize가 음수",
						new byte[]{1},
						"image/png",
						-1L,
						"sanitized image fileSize must be positive"
				)
		);
	}
}