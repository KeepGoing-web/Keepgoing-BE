package com.keepgoing.keepgoing.ai.repository;

import com.keepgoing.keepgoing.ai.domain.AiNoteChunk;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiNoteChunkRepository extends JpaRepository<AiNoteChunk, Long> {

	void deleteByNoteId(Long noteId);

	@Query(value = """
  		SELECT
  			c.note_id AS noteId,
  			c.title AS title,
  			c.content_chunk AS excerpt
  		FROM ai_note_chunks c
  		JOIN ai_note_indexes i ON i.note_id = c.note_id
  		JOIN notes n ON n.id = c.note_id
  		WHERE c.author_id = :authorId
  		  AND i.author_id = :authorId
  		  AND n.author_id = :authorId
  		  AND i.status = 'COMPLETED'
  		  AND n.deleted_at IS NULL
  		  AND n.ai_collectable = true
  		  AND (:excludeNoteId IS NULL OR c.note_id <> :excludeNoteId)
  		  AND (
  			LOWER(c.title) LIKE CONCAT('%', LOWER(:keyword), '%')
  			OR LOWER(c.content_chunk) LIKE CONCAT('%', LOWER(:keyword), '%')
  		  )
  		ORDER BY
  			CASE
  				WHEN LOWER(c.title) LIKE CONCAT('%', LOWER(:keyword), '%') THEN 0
  				ELSE 1
  			END,
  			c.indexed_at DESC,
  			c.note_id DESC,
  			c.chunk_order ASC
  		LIMIT :limit
  		""", nativeQuery = true)
	List<AiNoteRetrievalView> searchRelevantChunks(
			@Param("authorId") Long authorId,
			@Param("keyword") String keyword,
			@Param("excludeNoteId") Long excludeNoteId,
			@Param("limit") int limit
	);
}
