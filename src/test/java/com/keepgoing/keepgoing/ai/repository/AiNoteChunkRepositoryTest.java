package com.keepgoing.keepgoing.ai.repository;

import com.keepgoing.keepgoing.ai.domain.AiNoteChunk;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndexStatus;
import com.keepgoing.keepgoing.global.config.JpaAuditingConfig;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AiNoteChunkRepositoryTest {

	private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 5, 28, 10, 0);
	private static final LocalDateTime INDEXED_AT = LocalDateTime.of(2026, 5, 28, 10, 5);
	private static final int DEFAULT_LIMIT = 20;

	@Autowired
	AiNoteChunkRepository aiNoteChunkRepository;

	@Autowired
	AiNoteIndexRepository aiNoteIndexRepository;

	@Autowired
	NoteRepository noteRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	EntityManager entityManager;

	User user;
	User otherUser;

	@BeforeEach
	void setUp() {
		user = userRepository.save(User.create("user@test.com", "사용자"));
		otherUser = userRepository.save(User.create("other@test.com", "다른 사용자"));
	}

	@Test
	@DisplayName("내 note + COMPLETED + aiCollectable=true + not deleted면 검색된다")
	void searchRelevantChunksReturnsCollectableCompletedOwnNote() {
		// given
		Note note = saveIndexedNote(
				user,
				"배포 회의",
				"금요일 배포 결정",
				true,
				AiNoteIndexStatus.COMPLETED,
				false,
				INDEXED_AT
		);

		flushAndClear();

		// when
		List<AiNoteRetrievalView> results = aiNoteChunkRepository.searchRelevantChunks(
				user.getId(),
				"배포",
				null,
				DEFAULT_LIMIT
		);

		// then
		assertThat(results)
				.extracting(AiNoteRetrievalView::getNoteId)
				.containsExactly(note.getId());
		assertThat(results.get(0).getTitle()).isEqualTo("배포 회의");
		assertThat(results.get(0).getExcerpt()).contains("금요일 배포 결정");
	}

	@Test
	@DisplayName("다른 사용자의 note는 검색 결과에서 제외된다")
	void excludesOtherUsersNote() {
		// given
		saveIndexedNote(
				otherUser,
				"배포 회의",
				"다른 사용자 배포 내용",
				true,
				AiNoteIndexStatus.COMPLETED,
				false,
				INDEXED_AT
		);

		flushAndClear();

		// when
		List<AiNoteRetrievalView> results = aiNoteChunkRepository.searchRelevantChunks(
				user.getId(),
				"배포",
				null,
				DEFAULT_LIMIT
		);

		// then
		assertThat(results).isEmpty();
	}

	@Test
	@DisplayName("aiCollectable=false note는 검색 결과에서 제외된다")
	void excludesNotCollectableNote() {
		// given
		saveIndexedNote(
				user,
				"배포 회의",
				"비수집 배포 내용",
				false,
				AiNoteIndexStatus.COMPLETED,
				false,
				INDEXED_AT
		);

		flushAndClear();

		// when
		List<AiNoteRetrievalView> results = aiNoteChunkRepository.searchRelevantChunks(
				user.getId(),
				"배포",
				null,
				DEFAULT_LIMIT
		);

		// then
		assertThat(results).isEmpty();
	}

	@Test
	@DisplayName("COMPLETED 상태가 아닌 index는 검색 결과에서 제외된다")
	void excludesNonCompletedIndex() {
		// given
		saveIndexedNote(
				user,
				"배포 회의",
				"PENDING 상태 배포 내용",
				true,
				AiNoteIndexStatus.PENDING,
				false,
				INDEXED_AT
		);

		flushAndClear();

		// when
		List<AiNoteRetrievalView> results = aiNoteChunkRepository.searchRelevantChunks(
				user.getId(),
				"배포",
				null,
				DEFAULT_LIMIT
		);

		// then
		assertThat(results).isEmpty();
	}

	@Test
	@DisplayName("삭제된 note는 검색 결과에서 제외된다")
	void excludesDeletedNote() {
		// given
		saveIndexedNote(
				user,
				"배포 회의",
				"삭제된 배포 내용",
				true,
				AiNoteIndexStatus.COMPLETED,
				true,
				INDEXED_AT
		);

		flushAndClear();

		// when
		List<AiNoteRetrievalView> results = aiNoteChunkRepository.searchRelevantChunks(
				user.getId(),
				"배포",
				null,
				DEFAULT_LIMIT
		);

		// then
		assertThat(results).isEmpty();
	}

	@Test
	@DisplayName("excludeNoteId로 전달한 note는 검색 결과에서 제외된다")
	void excludesContextNoteId() {
		// given
		Note contextNote = saveIndexedNote(
				user,
				"배포 회의",
				"context note 배포 내용",
				true,
				AiNoteIndexStatus.COMPLETED,
				false,
				INDEXED_AT
		);
		Note anotherNote = saveIndexedNote(
				user,
				"배포 체크리스트",
				"다른 note 배포 내용",
				true,
				AiNoteIndexStatus.COMPLETED,
				false,
				INDEXED_AT.minusMinutes(1)
		);

		flushAndClear();

		// when
		List<AiNoteRetrievalView> results = aiNoteChunkRepository.searchRelevantChunks(
				user.getId(),
				"배포",
				contextNote.getId(),
				DEFAULT_LIMIT
		);

		// then
		assertThat(results)
				.extracting(AiNoteRetrievalView::getNoteId)
				.containsExactly(anotherNote.getId());
	}

	@Test
	@DisplayName("title match가 content match보다 먼저 정렬된다")
	void sortsTitleMatchBeforeContentMatch() {
		// given
		Note contentMatchNote = saveIndexedNote(
				user,
				"회의록",
				"본문에 배포 키워드가 있는 노트",
				true,
				AiNoteIndexStatus.COMPLETED,
				false,
				INDEXED_AT.plusMinutes(10)
		);
		Note titleMatchNote = saveIndexedNote(
				user,
				"배포 계획",
				"본문에는 다른 내용",
				true,
				AiNoteIndexStatus.COMPLETED,
				false,
				INDEXED_AT
		);

		flushAndClear();

		// when
		List<AiNoteRetrievalView> results = aiNoteChunkRepository.searchRelevantChunks(
				user.getId(),
				"배포",
				null,
				DEFAULT_LIMIT
		);

		// then
		assertThat(results)
				.extracting(AiNoteRetrievalView::getNoteId)
				.containsExactly(titleMatchNote.getId(), contentMatchNote.getId());
	}

	private Note saveIndexedNote(
			User author,
			String title,
			String contentChunk,
			boolean aiCollectable,
			AiNoteIndexStatus status,
			boolean deleted,
			LocalDateTime indexedAt
	) {
		Note note = noteRepository.save(Note.create(
				author,
				null,
				title,
				contentChunk,
				NoteVisibility.PRIVATE,
				aiCollectable
		));

		if (deleted) {
			note.softDelete();
		}

		AiNoteIndex index = AiNoteIndex.pending(note.getId(), author.getId(), REQUESTED_AT);
		applyStatus(index, status, indexedAt);
		aiNoteIndexRepository.save(index);

		aiNoteChunkRepository.save(AiNoteChunk.create(
				note.getId(),
				author.getId(),
				0,
				title,
				contentChunk,
				REQUESTED_AT,
				indexedAt
		));

		return note;
	}

	private void applyStatus(
			AiNoteIndex index,
			AiNoteIndexStatus status,
			LocalDateTime processedAt
	) {
		if (status == AiNoteIndexStatus.COMPLETED) {
			index.markCompleted(processedAt);
			return;
		}

		if (status == AiNoteIndexStatus.FAILED) {
			index.markFailed(processedAt, "indexing failed");
			return;
		}

		if (status == AiNoteIndexStatus.REMOVED) {
			index.markRemoved(processedAt);
		}
	}

	private void flushAndClear() {
		entityManager.flush();
		entityManager.clear();
	}
}
