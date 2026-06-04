package com.keepgoing.keepgoing.note.controller.validation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ValidImageFileValidatorTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUp() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		validatorFactory.close();
	}

	@Nested
	@DisplayName("@ValidImageFile")
	class ValidateImageFile {

		@Test
		@DisplayName("null은 통과시키고 필수 여부는 @NotNull에 맡긴다")
		void returnsValidWhenFileIsNull() {
			// given
			ImageFileOnly target = new ImageFileOnly(null);

			// when & then
			assertThat(validator.validate(target)).isEmpty();
		}

		@Test
		@DisplayName("빈 파일이면 검증에 실패한다")
		void returnsInvalidWhenFileIsEmpty() {
			// given
			ImageFileOnly target = new ImageFileOnly(new MockMultipartFile(
					"file",
					"image.png",
					MediaType.IMAGE_PNG_VALUE,
					new byte[0]
			));

			// when & then
			assertViolationMessage(target, "이미지 파일은 비어 있을 수 없습니다.");
		}

		@Test
		@DisplayName("파일명이 비어 있으면 검증에 실패한다")
		void returnsInvalidWhenOriginalFilenameIsBlank() {
			// given
			ImageFileOnly target = new ImageFileOnly(new MockMultipartFile(
					"file",
					" ",
					MediaType.IMAGE_PNG_VALUE,
					"image-content".getBytes(UTF_8)
			));

			// when & then
			assertViolationMessage(target, "이미지 파일명은 비어 있을 수 없습니다.");
		}

		@Test
		@DisplayName("Content-Type이 없으면 검증에 실패한다")
		void returnsInvalidWhenContentTypeIsMissing() {
			// given
			ImageFileOnly target = new ImageFileOnly(new MockMultipartFile(
					"file",
					"image.png",
					null,
					"image-content".getBytes(UTF_8)
			));

			// when & then
			assertViolationMessage(target, "이미지 Content-Type은 비어 있을 수 없습니다.");
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("unsupportedImageFiles")
		@DisplayName("허용 목록에 없는 Content-Type이면 검증에 실패한다")
		void returnsInvalidWhenContentTypeIsNotAllowed(
				String name,
				String originalFilename,
				String contentType
		) {
			// given
			ImageFileOnly target = new ImageFileOnly(new MockMultipartFile(
					"file",
					originalFilename,
					contentType,
					"image-content".getBytes(UTF_8)
			));

			// when & then
			assertViolationMessage(target, "지원하지 않는 이미지 형식입니다.");
		}

		@Test
		@DisplayName("파일 크기가 제한을 넘으면 검증에 실패한다")
		void returnsInvalidWhenFileSizeExceedsLimit() {
			// given
			LimitedImageFile target = new LimitedImageFile(new MockMultipartFile(
					"file",
					"image.png",
					MediaType.IMAGE_PNG_VALUE,
					"too-large".getBytes(UTF_8)
			));

			// when & then
			assertViolationMessage(target, "이미지 파일 크기가 허용 범위를 초과했습니다.");
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("allowedImageFiles")
		@DisplayName("허용된 이미지 Content-Type이면 검증을 통과한다")
		void returnsValidWhenContentTypeIsAllowed(
				String name,
				String originalFilename,
				String contentType
		) {
			// given
			ImageFileOnly target = new ImageFileOnly(new MockMultipartFile(
					"file",
					originalFilename,
					contentType,
					"image-content".getBytes(UTF_8)
			));

			// when & then
			assertThat(validator.validate(target)).isEmpty();
		}

		static Stream<Arguments> allowedImageFiles() {
			return Stream.of(
					Arguments.of(".jpg 파일 + image/jpeg", "image.jpg", MediaType.IMAGE_JPEG_VALUE),
					Arguments.of(".jpeg 파일 + image/jpeg", "image.jpeg", MediaType.IMAGE_JPEG_VALUE),
					Arguments.of(".png 파일 + image/png", "image.png", MediaType.IMAGE_PNG_VALUE),
					Arguments.of(".webp 파일 + image/webp", "image.webp", "image/webp"),
					Arguments.of("대소문자 MIME 정규화", "image.jpg", "IMAGE/JPEG"),
					Arguments.of("앞뒤 공백 MIME 정규화", "image.png", " image/png ")
			);
		}

		static Stream<Arguments> unsupportedImageFiles() {
			return Stream.of(
					Arguments.of("text/plain", "image.txt", MediaType.TEXT_PLAIN_VALUE),
					Arguments.of("비표준 image/jpg", "image.jpg", "image/jpg"),
					Arguments.of("GIF", "image.gif", "image/gif"),
					Arguments.of("SVG", "image.svg", "image/svg+xml"),
					Arguments.of("AVIF", "image.avif", "image/avif")
			);
		}
	}

	@Nested
	@DisplayName("@NotNull과 함께 사용할 때")
	class ValidateRequiredImageFile {

		@Test
		@DisplayName("파일이 없으면 필수 입력 검증에 실패한다")
		void returnsInvalidWhenRequiredFileIsNull() {
			// given
			RequiredImageFile target = new RequiredImageFile(null);

			// when & then
			assertThat(validator.validate(target))
					.extracting(violation -> violation.getPropertyPath().toString())
					.containsExactly("file");
		}
	}

	private void assertViolationMessage(Object target, String expectedMessage) {
		Set<ConstraintViolation<Object>> violations = validator.validate(target);

		assertThat(violations)
				.hasSize(1)
				.extracting(ConstraintViolation::getMessage)
				.containsExactly(expectedMessage);
	}

	private record ImageFileOnly(
			@ValidImageFile
			MultipartFile file
	) {
	}

	private record LimitedImageFile(
			@ValidImageFile(maxSize = 5)
			MultipartFile file
	) {
	}

	private record RequiredImageFile(
			@NotNull
			@ValidImageFile
			MultipartFile file
	) {
	}
}
