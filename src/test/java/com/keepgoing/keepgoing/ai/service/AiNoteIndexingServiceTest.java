package com.keepgoing.keepgoing.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.keepgoing.keepgoing.activity.service.ActivityDashboardService;
import com.keepgoing.keepgoing.ai.domain.AiNoteChunk;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndexStatus;
import com.keepgoing.keepgoing.ai.repository.AiNoteChunkRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexingSourceView;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiNoteIndexingServiceTest {

	@Mock
	NoteRepository noteRepository;

	@Mock
	AiNoteIndexRepository aiNoteIndexRepository;

	@Mock
	AiNoteChunkRepository aiNoteChunkRepository;

	@Spy
	AiNoteChunker aiNoteChunker = new AiNoteChunker();

	@Spy
	Clock clock = Clock.fixed(
			Instant.parse("2026-05-14T00:00:00Z"),
			ActivityDashboardService.KST
	);

	@InjectMocks
	AiNoteIndexingService aiNoteIndexingService;

	@Nested
	@DisplayName("인덱싱 처리")
	class Process {

		@Test
		@DisplayName("수집 가능한 노트면 chunk를 저장하고 COMPLETED로 마킹한다")
		void marksCompletedWhenCollectable() {
			// given
			Long noteId = 10L;
			Long authorId = 1L;
			LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 14, 9, 0);

			AiNoteIndex index = AiNoteIndex.pending(noteId, authorId, requestedAt);

			given(aiNoteIndexRepository.findById(noteId)).willReturn(Optional.of(index));
			given(noteRepository.findAiIndexingSourceById(noteId))
					.willReturn(Optional.of(source(
							noteId,
							authorId,
							"회의록",
							"오늘 논의한 내용이 길게 들어간다. ".repeat(30),
							true,
							null,
							requestedAt.plusHours(1)
					)));
			given(aiNoteChunkRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

			// when
			aiNoteIndexingService.process(noteId);

			// then
			verify(aiNoteChunkRepository).deleteByNoteId(noteId);

			ArgumentCaptor<List<AiNoteChunk>> chunkCaptor = ArgumentCaptor.forClass(List.class);
			verify(aiNoteChunkRepository).saveAll(chunkCaptor.capture());

			List<AiNoteChunk> savedChunks = chunkCaptor.getValue();
			assertThat(savedChunks).isNotEmpty();
			assertThat(savedChunks.get(0).getNoteId()).isEqualTo(noteId);
			assertThat(savedChunks.get(0).getAuthorId()).isEqualTo(authorId);
			assertThat(savedChunks.get(0).getTitle()).isEqualTo("회의록");

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.COMPLETED);
			assertThat(index.getIndexedAt()).isNotNull();
			assertThat(index.getLastError()).isNull();
			assertThat(index.getAttemptCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("삭제된 노트면 chunk를 제거하고 REMOVED로 마킹한다")
		void marksRemovedWhenDeleted() {
			// given
			Long noteId = 10L;
			Long authorId = 1L;
			LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 14, 9, 0);

			AiNoteIndex index = AiNoteIndex.pending(noteId, authorId, requestedAt);

			given(aiNoteIndexRepository.findById(noteId)).willReturn(Optional.of(index));
			given(noteRepository.findAiIndexingSourceById(noteId))
					.willReturn(Optional.of(source(
							noteId,
							authorId,
							"삭제된 노트",
							"내용",
							true,
							LocalDateTime.of(2026, 5, 14, 8, 0),
							requestedAt.plusHours(1)
					)));

			// when
			aiNoteIndexingService.process(noteId);

			// then
			verify(aiNoteChunkRepository).deleteByNoteId(noteId);
			verify(aiNoteChunkRepository, never()).saveAll(any());

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.REMOVED);
			assertThat(index.getIndexedAt()).isNull();
			assertThat(index.getAttemptCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("AI 수집 대상이 아니면 chunk를 제거하고 REMOVED로 마킹한다")
		void marksRemovedWhenNotCollectable() {
			// given
			Long noteId = 10L;
			Long authorId = 1L;
			LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 14, 9, 0);

			AiNoteIndex index = AiNoteIndex.pending(noteId, authorId, requestedAt);

			given(aiNoteIndexRepository.findById(noteId)).willReturn(Optional.of(index));
			given(noteRepository.findAiIndexingSourceById(noteId))
					.willReturn(Optional.of(source(
							noteId,
							authorId,
							"비수집 노트",
							"내용",
							false,
							null,
							requestedAt.plusHours(1)
					)));

			// when
			aiNoteIndexingService.process(noteId);

			// then
			verify(aiNoteChunkRepository).deleteByNoteId(noteId);
			verify(aiNoteChunkRepository, never()).saveAll(any());

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.REMOVED);
			assertThat(index.getAttemptCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("인덱싱 중 예외가 발생하면 FAILED로 마킹한다")
		void marksFailedWhenExceptionOccurs() {
			// given
			Long noteId = 10L;
			Long authorId = 1L;
			LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 14, 9, 0);

			AiNoteIndex index = AiNoteIndex.pending(noteId, authorId, requestedAt);

			given(aiNoteIndexRepository.findById(noteId)).willReturn(Optional.of(index));
			given(noteRepository.findAiIndexingSourceById(noteId))
					.willThrow(new RuntimeException("db read fail"));

			// when & then
			assertThatThrownBy(() -> aiNoteIndexingService.process(noteId))
					.isInstanceOf(RuntimeException.class)
					.hasMessageContaining("db read fail");

			assertThat(index.getStatus()).isEqualTo(AiNoteIndexStatus.FAILED);
			assertThat(index.getLastError()).contains("db read fail");
			assertThat(index.getAttemptCount()).isEqualTo(1);
		}

		@Test
		@DisplayName("인덱스가 없으면 예외가 발생한다")
		void throwsWhenIndexNotFound() {
			// given
			Long noteId = 10L;
			given(aiNoteIndexRepository.findById(noteId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> aiNoteIndexingService.process(noteId))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("AI note index not found");
		}
	}

	private AiNoteIndexingSourceView source(
			Long noteId,
			Long authorId,
			String title,
			String content,
			boolean aiCollectable,
			LocalDateTime deletedAt,
			LocalDateTime updatedAt
	) {
		return new AiNoteIndexingSourceView() {
			@Override
			public Long getNoteId() {
				return noteId;
			}

			@Override
			public Long getAuthorId() {
				return authorId;
			}

			@Override
			public String getTitle() {
				return title;
			}

			@Override
			public String getContent() {
				return content;
			}

			@Override
			public boolean isAiCollectable() {
				return aiCollectable;
			}

			@Override
			public LocalDateTime getDeletedAt() {
				return deletedAt;
			}

			@Override
			public LocalDateTime getUpdatedAt() {
				return updatedAt;
			}
		};
	}
}
