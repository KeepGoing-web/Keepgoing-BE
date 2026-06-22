package com.keepgoing.keepgoing.worker.infrastructure.imageio.adapter.out;

import com.keepgoing.keepgoing.worker.application.dto.PreValidatedImage;
import com.keepgoing.keepgoing.worker.application.dto.SanitizedImage;
import com.keepgoing.keepgoing.worker.application.port.out.ImageSanitizationException;
import com.keepgoing.keepgoing.worker.application.port.out.ImageSanitizerPort;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageIoImageSanitizerAdapter implements ImageSanitizerPort {

	private final int maxOutputWidth;
	private final int maxOutputHeight;
	private final long maxDecodePixels;

	public ImageIoImageSanitizerAdapter() {
		this(4096, 4096, 40_000_000L);
	}

	@Override
	public SanitizedImage sanitize(PreValidatedImage image, byte[] originalBytes) {
		String contentType = image.detectedContentType();

		validateDimensions(originalBytes, contentType);
		BufferedImage decodedImage = decode(originalBytes);
		// 픽셀 복사를 통해 원본 metadata와 파일 구조를 이어받지 않는다.
		BufferedImage cleanImage = copyPixels(decodedImage, contentType);
		BufferedImage resizedImage = resizeIfNeeded(cleanImage);
		byte[] sanitizedBytes = encode(resizedImage, contentType);

		return new SanitizedImage(
				sanitizedBytes,
				contentType,
				sanitizedBytes.length
		);
	}

	private BufferedImage decode(byte[] originalBytes) {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(originalBytes));
			if (image == null) {
				throw new ImageSanitizationException("이미지 디코딩에 실패했습니다.");
			}
			return image;
		} catch (IOException e) {
			throw new ImageSanitizationException("이미지 디코딩 중 오류가 발생했습니다.", e);
		}
	}

	private byte[] encode(BufferedImage image, String contentType) {
		String formatName = formatName(contentType);

		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			boolean written = ImageIO.write(image, formatName, outputStream);

			if (!written || outputStream.size() == 0) {
				throw new ImageSanitizationException("이미지 재인코딩에 실패했습니다.");
			}

			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new ImageSanitizationException("이미지 재인코딩 중 오류가 발생했습니다.", e);
		}
	}

	private BufferedImage copyPixels(BufferedImage source, String contentType) {
		int imageType = imageType(contentType);

		BufferedImage cleanImage = new BufferedImage(
				source.getWidth(),
				source.getHeight(),
				imageType
		);

		Graphics2D graphics = cleanImage.createGraphics();
		try {
			if (imageType == BufferedImage.TYPE_INT_RGB) {
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
			}
			graphics.drawImage(source, 0, 0, null);
			return cleanImage;
		} finally {
			graphics.dispose();
		}
	}

	private void validateDimensions(byte[] originalBytes, String contentType) {
		String formatName = formatName(contentType);

		try (ImageInputStream inputStream =
				     ImageIO.createImageInputStream(new ByteArrayInputStream(originalBytes))) {

			Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatName);
			if (!readers.hasNext()) {
				throw new ImageSanitizationException("지원하지 않는 이미지 디코더입니다: " + contentType);
			}
			ImageReader reader = readers.next();

			try {
				reader.setInput(inputStream, true, true);

				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				validateDecodePixelLimit(width, height);
			} finally {
				reader.dispose();
			}
		} catch (IOException e) {
			throw new ImageSanitizationException("이미지 크기 확인에 실패했습니다.", e);

		}
	}

	private void validateDecodePixelLimit(int width, int height) {
		long pixels = (long) width * height;

		if (width <= 0 || height <= 0) {
			throw new ImageSanitizationException("이미지 크기가 유효하지 않습니다.");
		}
		if (pixels > maxDecodePixels) {
			throw new ImageSanitizationException("이미지 픽셀 수가 허용 범위를 초과했습니다.");
		}
	}

	private BufferedImage resizeIfNeeded(BufferedImage source) {
		double scale = Math.min(
				(double) maxOutputWidth / source.getWidth(),
				(double) maxOutputHeight / source.getHeight()
		);

		if (scale >= 1.0) {
			return source;
		}

		int targetWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int targetHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));

		BufferedImage resized = new BufferedImage(
				targetWidth,
				targetHeight,
				source.getType()
		);

		Graphics2D graphics = resized.createGraphics();
		try {
			graphics.setRenderingHint(
					RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC
			);
			graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
			return resized;
		} finally {
			graphics.dispose();
		}
	}

	private int imageType(String contentType) {
		return switch (contentType) {
			case "image/jpeg" -> BufferedImage.TYPE_INT_RGB;
			case "image/png", "image/webp" -> BufferedImage.TYPE_INT_ARGB;
			default -> throw new ImageSanitizationException("지원하지 않는 이미지 출력 형식입니다: " + contentType);
		};
	}

	private String formatName(String contentType) {
		return switch (contentType) {
			case "image/png" -> "png";
			case "image/jpeg" -> "jpeg";
			case "image/webp" -> "webp";
			default -> throw new ImageSanitizationException("지원하지 않는 이미지 출력 형식입니다: " + contentType);
		};
	}
}
