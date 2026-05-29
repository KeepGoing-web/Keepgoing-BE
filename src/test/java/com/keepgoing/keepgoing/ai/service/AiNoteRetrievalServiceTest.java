package com.keepgoing.keepgoing.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.keepgoing.keepgoing.ai.repository.AiNoteChunkRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteRetrievalView;
import com.keepgoing.keepgoing.ai.service.dto.AiNoteRetrievalResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiNoteRetrievalServiceTest {

	@Mock
	AiNoteChunkRepository aiNoteChunkRepository;

	@InjectMocks
	AiNoteRetrievalService aiNoteRetrievalService;

	@Test
	@DisplayName("메시지가 null 또는 blank면 검색하지 않고 빈 결과를 반환한다")
	void returnsEmptyWhenMessageIsBlank() {
		assertThat(aiNoteRetrievalService.retrieve(1L, null, null)).isEmpty();
		assertThat(aiNoteRetrievalService.retrieve(1L, "   ", null)).isEmpty();

		verify(aiNoteChunkRepository, never()).searchRelevantChunks(any(), any(), any(), any(Integer.class));
	}

	@Test
	@DisplayName("후보 chunk를 noteId 기준으로 중복 제거하고 정렬 순서를 유지한다")
	void deduplicatesCandidatesByNoteId() {
		// given
		Long userId = 1L;
		Long contextNoteId = 10L;
		given(aiNoteChunkRepository.searchRelevantChunks(userId, "배포", contextNoteId, 20))
				.willReturn(List.of(
						view(1L, "첫 번째 노트", "첫 번째 chunk"),
						view(1L, "첫 번째 노트", "같은 노트의 두 번째 chunk"),
						view(2L, "두 번째 노트", "두 번째 노트 chunk")
				));

		// when
		List<AiNoteRetrievalResult> results = aiNoteRetrievalService.retrieve(userId, "  배포  ", contextNoteId);

		// then
		assertThat(results)
				.extracting(AiNoteRetrievalResult::noteId)
				.containsExactly(1L, 2L);
		assertThat(results.get(0).excerpt()).isEqualTo("첫 번째 chunk");
		verify(aiNoteChunkRepository).searchRelevantChunks(userId, "배포", contextNoteId, 20);
	}

	@Test
	@DisplayName("최종 retrieval 결과는 최대 5개까지만 반환한다")
	void limitsFinalResultsToFiveNotes() {
		// given
		Long userId = 1L;
		List<AiNoteRetrievalView> candidates = new ArrayList<>();
		for (long noteId = 1; noteId <= 7; noteId++) {
			candidates.add(view(noteId, "노트 " + noteId, "내용 " + noteId));
		}

		given(aiNoteChunkRepository.searchRelevantChunks(userId, "검색어", null, 20))
				.willReturn(candidates);

		// when
		List<AiNoteRetrievalResult> results = aiNoteRetrievalService.retrieve(userId, "검색어", null);

		// then
		assertThat(results)
				.hasSize(5)
				.extracting(AiNoteRetrievalResult::noteId)
				.containsExactly(1L, 2L, 3L, 4L, 5L);
	}

	@Test
	@DisplayName("excerpt가 길면 citation에 사용할 수 있도록 잘라서 반환한다")
	void truncatesLongExcerpt() {
		// given
		String longExcerpt = "a".repeat(501);
		given(aiNoteChunkRepository.searchRelevantChunks(1L, "검색어", null, 20))
				.willReturn(List.of(view(1L, "긴 노트", longExcerpt)));

		// when
		List<AiNoteRetrievalResult> results = aiNoteRetrievalService.retrieve(1L, "검색어", null);

		// then
		assertThat(results).hasSize(1);
		assertThat(results.get(0).excerpt())
				.hasSize(515)
				.endsWith("\n...(truncated)");
	}

	private AiNoteRetrievalView view(Long noteId, String title, String excerpt) {
		return new AiNoteRetrievalView() {
			@Override
			public Long getNoteId() {
				return noteId;
			}

			@Override
			public String getTitle() {
				return title;
			}

			@Override
			public String getExcerpt() {
				return excerpt;
			}
		};
	}
}
