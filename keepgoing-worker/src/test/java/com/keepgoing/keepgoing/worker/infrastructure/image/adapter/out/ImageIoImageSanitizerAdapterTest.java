package com.keepgoing.keepgoing.worker.infrastructure.image.adapter.out;

import static com.keepgoing.keepgoing.worker.support.ImageFixture.CONTENT_TYPE_JPEG;
import static com.keepgoing.keepgoing.worker.support.ImageFixture.CONTENT_TYPE_PNG;
import static com.keepgoing.keepgoing.worker.support.ImageFixture.CONTENT_TYPE_WEBP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keepgoing.keepgoing.worker.image.application.dto.PreValidatedImage;
import com.keepgoing.keepgoing.worker.image.application.dto.SanitizedImage;
import com.keepgoing.keepgoing.worker.support.ImageFixture;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageIoImageSanitizerAdapterTest {

	public static final int MAX_OUTPUT_WIDTH = 100;
	public static final int MAX_OUTPUT_HEIGHT = 100;
	private static final long DEFAULT_MAX_DECODE_PIXELS = 10_000_000L;
	private static final long SMALL_MAX_DECODE_PIXELS = 10_000L;
	private static final String METADATA_MARKER = "keepgoing-test-metadata";

	private final ImageIoImageSanitizerAdapter sanitizerAdapter
			= new ImageIoImageSanitizerAdapter(MAX_OUTPUT_WIDTH, MAX_OUTPUT_HEIGHT, DEFAULT_MAX_DECODE_PIXELS);

	@Test
	@DisplayName("PNG 이미지를 디코딩 후 다시 PNG로 인코딩한다")
	void sanitizesPngImage() throws Exception {
		// given
		byte[] originalBytes = ImageFixture.decodablePngBytes();
		PreValidatedImage image = preValidatedImage("image/png", originalBytes.length);

		// when
		SanitizedImage result = sanitizerAdapter.sanitize(image, originalBytes);

		// then
		assertThat(result.contentType()).isEqualTo(CONTENT_TYPE_PNG);
		assertThat(result.bytes()).isNotEmpty();
		assertThat(result.fileSize()).isEqualTo(result.bytes().length);

		BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(decodedResult).isNotNull();
		assertThat(decodedResult.getWidth()).isEqualTo(1);
		assertThat(decodedResult.getHeight()).isEqualTo(1);
	}

	@Test
	@DisplayName("JPEG 이미지를 디코딩 후 다시 JPEG로 인코딩한다")
	void sanitizesJpegImage() throws Exception {
		// given
		byte[] originalBytes = ImageFixture.decodableJpegBytes();
		PreValidatedImage image = preValidatedImage(CONTENT_TYPE_JPEG, originalBytes.length);

		// when
		SanitizedImage result = sanitizerAdapter.sanitize(image, originalBytes);

		// then
		assertThat(result.contentType()).isEqualTo(CONTENT_TYPE_JPEG);
		assertThat(result.bytes()).isNotEmpty();
		assertThat(result.fileSize()).isEqualTo(result.bytes().length);

		BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(decodedResult).isNotNull();
		assertThat(decodedResult.getWidth()).isEqualTo(1);
		assertThat(decodedResult.getHeight()).isEqualTo(1);
	}

	@Test
	@DisplayName("디코딩할 수 없는 이미지이면 정제에 실패한다")
	void throwsWhenImageCannotBeDecoded() {
		// given
		byte[] brokenBytes = "not-image".getBytes(StandardCharsets.UTF_8);
		PreValidatedImage image = preValidatedImage("image/png", brokenBytes.length);

		// when & then
		assertThatThrownBy(() -> sanitizerAdapter.sanitize(image, brokenBytes))
				.isInstanceOf(ImageSanitizationException.class);
	}

	@Test
	@DisplayName("최대 출력 너비를 초과하면 비율을 유지해 리사이징한다")
	void resizesWhenImageWidthExceedsOutputLimit() throws Exception {
		// given
		byte[] originalBytes = ImageFixture.decodablePngBytes(200, 100);
		PreValidatedImage image = preValidatedImage(CONTENT_TYPE_PNG, originalBytes.length);

		// when
		SanitizedImage result = sanitizerAdapter.sanitize(image, originalBytes);

		// then
		BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(result.bytes()));

		assertThat(decodedResult).isNotNull();
		assertThat(decodedResult.getWidth()).isEqualTo(100);
		assertThat(decodedResult.getHeight()).isEqualTo(50);
	}

	@Test
	@DisplayName("최대 출력 높이를 초과하면 비율을 유지해 리사이징한다")
	void resizesWhenImageHeightExceedsOutputLimit() throws Exception {
		// given
		byte[] originalBytes = ImageFixture.decodablePngBytes(100, 200);
		PreValidatedImage image = preValidatedImage(CONTENT_TYPE_PNG, originalBytes.length);

		// when
		SanitizedImage result = sanitizerAdapter.sanitize(image, originalBytes);

		// then
		BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(result.bytes()));

		assertThat(decodedResult).isNotNull();
		assertThat(decodedResult.getWidth()).isEqualTo(50);
		assertThat(decodedResult.getHeight()).isEqualTo(100);
	}

	@Test
	@DisplayName("최대 출력 크기 이하면 리사이징하지 않는다")
	void doesNotResizeWhenImageIsWithinOutputLimit() throws Exception {
		// given
		byte[] originalBytes = ImageFixture.decodablePngBytes(80, 60);
		PreValidatedImage image = preValidatedImage(CONTENT_TYPE_PNG, originalBytes.length);

		// when
		SanitizedImage result = sanitizerAdapter.sanitize(image, originalBytes);

		// then
		BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(result.bytes()));

		assertThat(decodedResult).isNotNull();
		assertThat(decodedResult.getWidth()).isEqualTo(80);
		assertThat(decodedResult.getHeight()).isEqualTo(60);
	}

	@Test
	@DisplayName("최대 디코딩 픽셀 수를 초과하면 정제에 실패한다")
	void throwsWhenImagePixelsExceedDecodeLimit() {
		// given
		ImageIoImageSanitizerAdapter smallPixelLimitSanitizer
				= new ImageIoImageSanitizerAdapter(MAX_OUTPUT_WIDTH, MAX_OUTPUT_HEIGHT, SMALL_MAX_DECODE_PIXELS);
		byte[] originalBytes = ImageFixture.decodablePngBytes(121, 100);
		PreValidatedImage image = preValidatedImage(CONTENT_TYPE_PNG, originalBytes.length);

		// when & then
		assertThatThrownBy(() -> smallPixelLimitSanitizer.sanitize(image, originalBytes))
				.isInstanceOf(ImageSanitizationException.class);
	}

	@Test
	@DisplayName("WebP 이미지를 디코딩 후 다시 WebP로 인코딩한다")
	void sanitizesWebpImage() throws Exception {
		// given
		byte[] originalBytes = ImageFixture.decodableWebpBytes();
		PreValidatedImage image = preValidatedImage(CONTENT_TYPE_WEBP, originalBytes.length);

		// when
		SanitizedImage result = sanitizerAdapter.sanitize(image, originalBytes);

		// then
		assertThat(result.contentType()).isEqualTo(CONTENT_TYPE_WEBP);
		assertThat(result.bytes()).isNotEmpty();
		assertThat(result.fileSize()).isEqualTo(result.bytes().length);

		BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(decodedResult).isNotNull();
		assertThat(decodedResult.getWidth()).isEqualTo(1);
		assertThat(decodedResult.getHeight()).isEqualTo(1);
	}

	@Test
	@DisplayName("메타데이터가 포함된 JPEG는 픽셀만 복사해 재인코딩한다")
	void removesMetadataWhenReEncodingJpeg() throws Exception {
		// given
		byte[] originalBytes = ImageFixture.decodableJpegBytesWithMetadata(METADATA_MARKER);
		PreValidatedImage image = preValidatedImage(CONTENT_TYPE_JPEG, originalBytes.length);

		assertThat(binaryString(originalBytes)).contains(METADATA_MARKER);

		// when
		SanitizedImage result = sanitizerAdapter.sanitize(image, originalBytes);

		// then
		assertThat(result.contentType()).isEqualTo(CONTENT_TYPE_JPEG);
		assertThat(binaryString(result.bytes())).doesNotContain(METADATA_MARKER);

		BufferedImage decodedResult = ImageIO.read(new ByteArrayInputStream(result.bytes()));
		assertThat(decodedResult).isNotNull();
	}

	private static String binaryString(byte[] bytes) {
		return new String(bytes, StandardCharsets.ISO_8859_1);
	}

	private static PreValidatedImage preValidatedImage(String contentType, long fileSize) {
		return new PreValidatedImage(
				UUID.randomUUID(),
				"notes/1/image",
				contentType,
				contentType,
				fileSize,
				Instant.parse("2026-05-15T00:00:00Z")
		);
	}
}