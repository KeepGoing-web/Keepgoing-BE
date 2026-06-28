package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.domain.AiNoteChunk;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.repository.AiNoteChunkRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexingSourceView;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiNoteIndexingProcessor {

	private final NoteRepository noteRepository;
	private final AiNoteIndexRepository aiNoteIndexRepository;
	private final AiNoteChunkRepository aiNoteChunkRepository;
	private final AiNoteChunker aiNoteChunker;
	private final Clock clock;

	@Transactional
	public void processInTransaction(Long noteId) {
		AiNoteIndex index = aiNoteIndexRepository.findByNoteIdForUpdate(noteId)
				.orElseThrow(() -> new IllegalStateException("AI note index not found: " + noteId));

		LocalDateTime processedAt = LocalDateTime.now(clock);

		AiNoteIndexingSourceView source = noteRepository.findAiIndexingSourceById(noteId)
				.orElse(null);

		if (source == null || source.getDeletedAt() != null || !source.isAiCollectable()) {
			aiNoteChunkRepository.deleteByNoteId(noteId);
			index.markRemoved(processedAt);
			return;
		}

		List<String> chunks = aiNoteChunker.split(source.getTitle(), source.getContent());

		aiNoteChunkRepository.deleteByNoteId(noteId);
		aiNoteChunkRepository.saveAll(createChunks(source, chunks, processedAt));

		index.markCompleted(processedAt);
	}

	private List<AiNoteChunk> createChunks(
			AiNoteIndexingSourceView source,
			List<String> chunks,
			LocalDateTime indexedAt
	) {
		List<AiNoteChunk> results = new ArrayList<>();

		for (int i = 0; i < chunks.size(); i++) {
			results.add(AiNoteChunk.create(
					source.getNoteId(),
					source.getAuthorId(),
					i,
					source.getTitle(),
					chunks.get(i),
					source.getUpdatedAt(),
					indexedAt
			));
		}
		return results;
	}
}
