package com.keepgoing.keepgoing.note.repository;

import com.keepgoing.keepgoing.note.domain.NoteImage;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NoteImageRepository extends JpaRepository<NoteImage, Long> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE NoteImage ni
			SET ni.deletedAt = CURRENT_TIMESTAMP,
				ni.updatedAt = CURRENT_TIMESTAMP
			WHERE ni.note.id = :noteId
				AND ni.deletedAt IS NULL
			""")
	int softDeleteByNoteId(@Param("noteId") Long noteId);

	Optional<NoteImage> findByPublicId(UUID publicId);
}
