package com.keepgoing.keepgoing.ai.repository;

import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndexStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiNoteIndexRepository extends JpaRepository<AiNoteIndex, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from AiNoteIndex i where i.noteId = :noteId")
	Optional<AiNoteIndex> findByNoteIdForUpdate(@Param("noteId") Long noteId);

	@Query("""
			select i from AiNoteIndex i
			where i.attemptCount < :maxAttemptCount
			    and (
			        i.status = :failedStatus
			        or (
			            i.status = :pendingStatus
			            and i.lastRequestedAt <= :stalePendingThreshold
			        )
			    )
			order by i.lastRequestedAt asc
			""")
	List<AiNoteIndex> findRetryTargets(
			@Param("failedStatus") AiNoteIndexStatus failedStatus,
			@Param("pendingStatus") AiNoteIndexStatus pendingStatus,
			@Param("maxAttemptCount") int maxAttemptCount,
			@Param("stalePendingThreshold") LocalDateTime stalePendingThreshold,
			Pageable pageable
	);

	@Modifying
	@Query(value = """
		insert into ai_note_indexes (
			note_id,
			author_id,
			status,
			attempt_count,
			last_requested_at,
			last_error
		)
		values (
			:noteId,
			:authorId,
			'PENDING',
			0,
			:requestedAt,
			null
		)
		on conflict (note_id) do update
		set author_id = excluded.author_id,
			status = 'PENDING',
			attempt_count = 0,
			last_requested_at = excluded.last_requested_at,
			last_error = null
		""", nativeQuery = true)
	void upsertPending(
			@Param("noteId") Long noteId,
			@Param("authorId") Long authorId,
			@Param("requestedAt") LocalDateTime requestedAt
	);
}
