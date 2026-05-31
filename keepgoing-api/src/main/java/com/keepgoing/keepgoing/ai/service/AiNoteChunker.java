package com.keepgoing.keepgoing.ai.service;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class AiNoteChunker {

	static final int MAX_CHUNK_LENGTH = 700;
	static final int CHUNK_OVERLAP = 120;

	public List<String> split(String title, String content) {
		String normalizedTitle = normalize(title);
		String normalizedContent = normalize(content);

		String source = normalizedTitle.isBlank()
				? normalizedContent
				: normalizedTitle + "\n\n" + normalizedContent;

		if (source.isBlank()) {
			return List.of();
		}

		List<String> chunks = new ArrayList<>();
		int cursor = 0;

		while (cursor < source.length()) {
			int end = Math.min(source.length(), cursor + MAX_CHUNK_LENGTH);
			chunks.add(source.substring(cursor, end).trim());

			if (end == source.length()) {
				break;
			}
			cursor = Math.max(end - CHUNK_OVERLAP, cursor + 1);
		}

		return chunks;
	}

	private String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().replace("\r\n", "\n");
	}
}
