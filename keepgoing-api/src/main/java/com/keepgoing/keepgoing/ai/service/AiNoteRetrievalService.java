package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.repository.AiNoteChunkRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteRetrievalView;
import com.keepgoing.keepgoing.ai.service.dto.AiNoteRetrievalResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiNoteRetrievalService {

	private static final int MAX_RETRIEVED_NOTES = 5;
	private static final int CANDIDATE_CHUNK_LIMIT = 20;
	private static final int MAX_EXCERPT_LENGTH = 500;

	private final AiNoteChunkRepository aiNoteChunkRepository;

	public List<AiNoteRetrievalResult> retrieve(
			Long userId,
			String message,
			Long contextNoteId
	) {
		String keyword = normalizeKeyword(message);

		if (keyword.isBlank()) {
			return List.of();
		}

		List<AiNoteRetrievalView> candidates = aiNoteChunkRepository.searchRelevantChunks(
				userId,
				keyword,
				contextNoteId,
				CANDIDATE_CHUNK_LIMIT
		);

		return deduplicateByNoteId(candidates);
	}

	private List<AiNoteRetrievalResult> deduplicateByNoteId(List<AiNoteRetrievalView> candidates) {
		Map<Long, AiNoteRetrievalResult> resultsByNoteId = new LinkedHashMap<>();

		for (AiNoteRetrievalView candidate : candidates) {
			resultsByNoteId.putIfAbsent(
					candidate.getNoteId(),
					new AiNoteRetrievalResult(
							candidate.getNoteId(),
							candidate.getTitle(),
							truncateExcerpt(candidate.getExcerpt())
					)
			);

			if (resultsByNoteId.size() >= MAX_RETRIEVED_NOTES) {
				break;
			}
		}
		return List.copyOf(resultsByNoteId.values());
	}

	private String normalizeKeyword(String message) {
		if (message == null) {
			return "";
		}

		return message.trim();
	}

	private String truncateExcerpt(String excerpt) {
		if (excerpt == null || excerpt.isBlank()) {
			return "";
		}

		if (excerpt.length() <= MAX_EXCERPT_LENGTH) {
			return excerpt;
		}

		return excerpt.substring(0, MAX_EXCERPT_LENGTH) + "\n...(truncated)";
	}
}
