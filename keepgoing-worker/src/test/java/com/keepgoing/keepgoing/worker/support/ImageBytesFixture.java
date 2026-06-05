package com.keepgoing.keepgoing.worker.support;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class ImageBytesFixture {

	private ImageBytesFixture() {
	}

	public static byte[] jpegBytes() {
		return new byte[]{
				(byte) 0xFF, (byte) 0xD8, (byte) 0xFF,
				0x00, 0x00, 0x00
		};
	}

	public static byte[] pngBytes() {
		return new byte[]{
				(byte) 0x89, 0x50, 0x4E, 0x47,
				0x0D, 0x0A, 0x1A, 0x0A,
				0x00, 0x00
		};
	}

	public static byte[] decodableJpegBytes() {
		return imageBytes("jpeg", BufferedImage.TYPE_INT_RGB);
	}

	public static byte[] decodableJpegBytes(int width, int height) {
		return imageBytes("jpeg", BufferedImage.TYPE_INT_RGB, width, height);
	}

	public static byte[] decodablePngBytes() {
		return imageBytes("png", BufferedImage.TYPE_INT_ARGB);
	}

	public static byte[] decodablePngBytes(int width, int height) {
		return imageBytes("png", BufferedImage.TYPE_INT_ARGB, width, height);
	}

	private static byte[] imageBytes(String formatName, int imageType) {
		try {
			BufferedImage image = new BufferedImage(1, 1, imageType);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			boolean written = ImageIO.write(image, formatName, outputStream);
			if (!written || outputStream.size() == 0) {
				throw new IllegalStateException("테스트 이미지 생성 실패: " + formatName);
			}

			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("테스트 이미지 생성 실패: " + formatName, e);
		}
	}

	private static byte[] imageBytes(String formatName, int imageType, int width, int height) {
		try {
			BufferedImage image = new BufferedImage(width, height, imageType);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			boolean written = ImageIO.write(image, formatName, outputStream);
			if (!written || outputStream.size() == 0) {
				throw new IllegalStateException("테스트 이미지 생성 실패: " + formatName);
			}

			return outputStream.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("테스트 이미지 생성 실패: " + formatName, e);
		}
	}
}
