package com.keepgoing.keepgoing.ai.repository;

import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiNoteIndexRepository extends JpaRepository<AiNoteIndex, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from AiNoteIndex i where i.noteId = :noteId")
	Optional<AiNoteIndex> findByNoteIdForUpdate(@Param("noteId") Long noteId);
}
