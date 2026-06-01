package com.keepgoing.keepgoing.ai;

import com.keepgoing.keepgoing.support.PostgreSqlTestContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.keepgoing.keepgoing.ai.domain.AiNoteChunk;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndexStatus;
import com.keepgoing.keepgoing.ai.repository.AiNoteChunkRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.NoteCreateCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import com.keepgoing.keepgoing.note.service.dto.NoteRenameCommand;
import com.keepgoing.keepgoing.note.service.dto.NoteUpdateCommand;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AiNoteIndexingIntegrationTest extends PostgreSqlTestContainerSupport {

	private static final int MAX_ATTEMPTS = 30;
	private static final long WAIT_MILLIS = 100L;


	@Autowired
	NoteService noteService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	AiNoteIndexRepository aiNoteIndexRepository;

	@Autowired
	AiNoteChunkRepository aiNoteChunkRepository;

	@MockitoBean
	ChatClient aiPanelChatClient;

	@Test
	@DisplayName("노트 생성 커밋 후 AI 인덱스와 청크를 생성한다")
	void indexesNoteAfterCreateCommit() {
		// given
		User user = saveUser("create-index@test.com", "생성 사용자");

		// when
		NoteDetailResult note = noteService.createNote(new NoteCreateCommand(
				user.getId(),
				null,
				"AI 인덱싱 생성 테스트",
				"생성 커밋 이후 청크가 만들어져야 한다",
				NoteVisibility.PRIVATE,
				true
		));

		// then
		awaitAssert(() -> {
			AiNoteIndex index = findIndex(note.noteId());

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.COMPLETED);
			assertThat(index.getAttemptCount()).isEqualTo(1);
			assertThat(index.getLastError()).isNull();
			assertThat(index.getIndexedAt()).isNotNull();

			List<AiNoteChunk> chunks = findChunks(note.noteId());
			assertThat(chunks).hasSize(1);
			assertThat(chunks.get(0).getTitle()).isEqualTo("AI 인덱싱 생성 테스트");
			assertThat(chunks.get(0).getContentChunk())
					.contains("생성 커밋 이후 청크");
		});
	}

	@Test
	@DisplayName("노트 수정 커밋 후 기존 청크를 새 내용으로 재색인한다")
	void reindexesNoteAfterUpdateCommit() {
		// given
		User user = saveUser("update-index@test.com", "수정 사용자");
		NoteDetailResult note = createCollectableNote(user, "수정 전 제목", "수정 전 본문");

		awaitCompleted(note.noteId(), "수정 전 본문");

		// when
		noteService.updateNote(new NoteUpdateCommand(
				note.noteId(),
				user.getId(),
				"수정 후 제목",
				"수정 후 본문 내용",
				NoteVisibility.PRIVATE,
				true
		));

		// then
		awaitAssert(() -> {
			AiNoteIndex index = findIndex(note.noteId());

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.COMPLETED);
			assertThat(index.getAttemptCount()).isEqualTo(1);
			assertThat(index.getLastError()).isNull();

			List<AiNoteChunk> chunks = findChunks(note.noteId());
			assertThat(chunks).hasSize(1);
			assertThat(chunks.get(0).getTitle()).isEqualTo("수정 후 제목");
			assertThat(chunks.get(0).getContentChunk()).contains("수정 후 본문 내용");
			assertThat(chunks.get(0).getContentChunk()).doesNotContain("수정 전 본문");
		});
	}

	@Test
	@DisplayName("노트 제목 변경 커밋 후 청크 제목을 재색인한다")
	void reindexesNoteAfterRenameCommit() {
		// given
		User user = saveUser("rename-index@test.com", "이름변경 사용자");
		NoteDetailResult note = createCollectableNote(user, "변경 전 제목", "제목 변경 본문");

		awaitCompleted(note.noteId(), "제목 변경 본문");

		// when
		noteService.renameNote(new NoteRenameCommand(
				note.noteId(),
				user.getId(),
				"변경 후 제목"
		));

		// then
		awaitAssert(() -> {
			AiNoteIndex index = findIndex(note.noteId());

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.COMPLETED);
			assertThat(index.getLastError()).isNull();

			List<AiNoteChunk> chunks = findChunks(note.noteId());
			assertThat(chunks).hasSize(1);
			assertThat(chunks.get(0).getTitle()).isEqualTo("변경 후 제목");
			assertThat(chunks.get(0).getContentChunk()).contains("제목 변경 본문");
		});
	}

	@Test
	@DisplayName("노트 삭제 커밋 후 인덱스를 REMOVED로 바꾸고 청크를 삭제한다")
	void removesIndexAndChunksAfterDeleteCommit() {
		// given
		User user = saveUser("delete-index@test.com", "삭제 사용자");
		NoteDetailResult note = createCollectableNote(user, "삭제 테스트", "삭제 전 청크");

		awaitCompleted(note.noteId(), "삭제 전 청크");

		// when
		noteService.deleteNote(user.getId(), note.noteId());

		// then
		awaitAssert(() -> {
			AiNoteIndex index = findIndex(note.noteId());

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.REMOVED);
			assertThat(index.getLastError()).isNull();
			assertThat(index.getIndexedAt()).isNull();
			assertThat(findChunks(note.noteId())).isEmpty();
		});
	}

	private User saveUser(String email, String name) {
		return userRepository.save(User.create(email, name));
	}

	private NoteDetailResult createCollectableNote(User user, String title, String content) {
		return noteService.createNote(new NoteCreateCommand(
				user.getId(),
				null,
				title,
				content,
				NoteVisibility.PRIVATE,
				true
		));
	}

	private void awaitCompleted(Long noteId, String expectedContent) {
		awaitAssert(() -> {
			AiNoteIndex index = findIndex(noteId);

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.COMPLETED);
			assertThat(findChunks(noteId))
					.anySatisfy(chunk -> assertThat(chunk.getContentChunk()).contains(expectedContent));
		});
	}

	private AiNoteIndex findIndex(Long noteId) {
		return aiNoteIndexRepository.findById(noteId).orElseThrow();
	}

	private List<AiNoteChunk> findChunks(Long noteId) {
		return aiNoteChunkRepository.findAll()
				.stream()
				.filter(chunk -> chunk.getNoteId().equals(noteId))
				.toList();
	}

	private void awaitAssert(Runnable assertion) {
		Throwable lastFailure = null;

		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			try {
				assertion.run();
				return;
			} catch (AssertionError | RuntimeException failure) {
				lastFailure = failure;
				sleep();
			}
		}

		throw new AssertionError("Async AI note indexing did not complete in time.", lastFailure);
	}

	private void sleep() {
		try {
			Thread.sleep(WAIT_MILLIS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}
}
