package com.keepgoing.keepgoing.note.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.global.config.JpaAuditingConfig;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteImage;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class NoteImageRepositoryTest {

	@Autowired
	private NoteImageRepository noteImageRepository;

	@Autowired
	private NoteRepository noteRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager em;

	@Nested
	@DisplayName("softDeleteByNoteId")
	class SoftDeleteByNoteId {

		@Test
		@DisplayName("특정 노트의 활성 이미지만 soft delete한다")
		void softDeleteByNoteId_softDeletesOnlyActiveImagesOfNoteBy() {
			// given
			User author = userRepository.save(User.create("author@test.com", "작성자"));
			Note targetNote = noteRepository.save(note(author, "대상 노트"));
			Note otherNote = noteRepository.save(note(author, "다른 노트"));

			noteImageRepository.save(noteImage(targetNote, author, "note-images/target-1.png"));
			noteImageRepository.save(noteImage(targetNote, author, "note-images/target-2.png"));
			noteImageRepository.save(noteImage(otherNote, author, "note-images/other.png"));
			em.flush();
			em.clear();

			// when
			int updatedCount = noteImageRepository.softDeleteByNoteId(targetNote.getId());
			em.flush();
			em.clear();

			// then
			assertThat(updatedCount).isEqualTo(2);
			assertThat(countDeletedImages(targetNote.getId())).isEqualTo(2);
			assertThat(countActiveImages(otherNote.getId())).isEqualTo(1);
		}

		@Test
		@DisplayName("이미 삭제된 이미지는 updated count에 포함하지 않는다")
		void softDeleteByByNoteId_excludesAlreadyDeletedImagesFromUpdatedCount() {
			// given
			User author = userRepository.save(User.create("author@test.com", "작성자"));
			Note targetNote = noteRepository.save(note(author, "대상 노트"));

			NoteImage alreadyDeleted = noteImageRepository.save(
					noteImage(targetNote, author, "note-images/already-deleted.png")
			);
			noteImageRepository.save(noteImage(targetNote, author, "note-images/active.png"));
			alreadyDeleted.softDelete();
			em.flush();
			em.clear();

			// when
			int updatedCount = noteImageRepository.softDeleteByNoteId(targetNote.getId());
			em.flush();
			em.clear();

			// then
			assertThat(updatedCount).isEqualTo(1);
			assertThat(countDeletedImages(targetNote.getId())).isEqualTo(2);
		}

		@Test
		@DisplayName("영속성 컨텍스트에 이미지가 있어도 DB에 soft delete를 반영한다")
		void softDeleteByByNoteId_reflectsBulkUpdateWhenImageIsManaged() {
			// given
			User author = userRepository.save(User.create("author@test.com", "작성자"));
			Note targetNote = noteRepository.save(note(author, "대상 노트"));
			noteImageRepository.save(noteImage(targetNote, author, "note-images/managed.png"));
			em.flush();

			// when
			int updatedCount = noteImageRepository.softDeleteByNoteId(targetNote.getId());
			em.flush();
			em.clear();

			// then
			assertThat(updatedCount).isEqualTo(1);
			assertThat(countDeletedImages(targetNote.getId())).isEqualTo(1);
			assertThat(noteImageRepository.findAll()).isEmpty();
		}
	}

	private Note note(User author, String title) {
		return Note.create(
				author,
				null,
				title,
				"노트 본문",
				NoteVisibility.PRIVATE,
				false
		);
	}

	private NoteImage noteImage(Note note, User uploader, String storageKey) {
		return NoteImage.create(
				note,
				uploader,
				storageKey,
				"image.png",
				"image/png",
				1024L
		);
	}

	private long countDeletedImages(Long noteId) {
		return ((Number) em.createNativeQuery("""
					select count(*)
					from note_images
					where note_id = :noteId
					  and deleted_at is not null
					""")
				.setParameter("noteId", noteId)
				.getSingleResult())
				.longValue();
	}

	private long countActiveImages(Long noteId) {
		return ((Number) em.createNativeQuery("""
					select count(*)
					from note_images
					where note_id = :noteId
					  and deleted_at is null
					""")
				.setParameter("noteId", noteId)
				.getSingleResult())
				.longValue();
	}
}
