package com.keepgoing.keepgoing.common.image.domain;

import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImageContentTypePolicy {

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/webp"
	);

	public static boolean isAllowed(String contentType) {
		String normalized = normalize(contentType);
		return normalized != null && ALLOWED_CONTENT_TYPES.contains(normalized);
	}

	public static String normalize(String contentType) {
		if (contentType == null) {
			return null;
		}
		return contentType.trim().toLowerCase(Locale.ROOT);
	}

	public static Set<String> allowedContentTypes() {
		return ALLOWED_CONTENT_TYPES;
	}
}
