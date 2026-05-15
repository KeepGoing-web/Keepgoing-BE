package com.keepgoing.keepgoing.ai.repository;

import com.keepgoing.keepgoing.ai.domain.AiNoteChunk;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiNoteChunkRepository extends JpaRepository<AiNoteChunk, Long> {

	void deleteByNoteId(Long noteId);
}
