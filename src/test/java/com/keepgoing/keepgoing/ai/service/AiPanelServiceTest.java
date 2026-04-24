package com.keepgoing.keepgoing.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageCommand;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
class AiPanelServiceTest {

	@Mock
	ChatClient aiPanelChatClient;

	@Mock
	ChatClient.ChatClientRequestSpec chatClientRequestSpec;

	@Mock
	ChatClient.CallResponseSpec callResponseSpec;

	@Mock
	NoteService noteService;

	@InjectMocks
	AiPanelService aiPanelService;

	@BeforeEach
	void setUp() {
		lenient().when(aiPanelChatClient.prompt()).thenReturn(chatClientRequestSpec);
		lenient().when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
		lenient().when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
	}

	@Test
	@DisplayName("contextNoteId가 없으면 사용자 메시지만 프롬프트에 포함한다")
	void sendsMessageWithoutNoteContext() {
		Long userId = 1L;
		AiPanelMessageCommand command = new AiPanelMessageCommand(null, "요약해줘");
		given(callResponseSpec.content()).willReturn("응답");

		AiPanelMessageResult result = aiPanelService.sendMessage(userId, command);

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(chatClientRequestSpec).user(promptCaptor.capture());
		assertThat(promptCaptor.getValue())
				.contains("요약해줘")
				.doesNotContain("회의록");
		assertThat(result.assistantMessage()).isEqualTo("응답");
		assertThat(result.contextNoteId()).isNull();
		assertThat(result.contextAttached()).isFalse();
		verifyNoInteractions(noteService);
	}

	@Test
	@DisplayName("contextNoteId가 있으면 노트 문맥을 프롬프트에 포함한다")
	void sendsMessageWithNoteContext() {
		Long userId = 1L;
		Long noteId = 10L;
		AiPanelMessageCommand command = new AiPanelMessageCommand(noteId, "이 노트를 요약해줘");
		NoteDetailResult note = new NoteDetailResult(
				noteId,
				20L,
				userId,
				"회의록",
				"오늘 논의한 내용",
				NoteVisibility.PRIVATE,
				true,
				null,
				null
		);
		given(noteService.getNote(userId, noteId)).willReturn(note);
		given(callResponseSpec.content()).willReturn("AI 응답");

		AiPanelMessageResult result = aiPanelService.sendMessage(userId, command);

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(chatClientRequestSpec).user(promptCaptor.capture());
		assertThat(promptCaptor.getValue())
				.contains("회의록")
				.contains("오늘 논의한 내용")
				.contains("이 노트를 요약해줘");
		assertThat(result.assistantMessage()).isEqualTo("AI 응답");
		assertThat(result.contextNoteId()).isEqualTo(noteId);
		assertThat(result.contextAttached()).isTrue();
		verify(noteService).getNote(userId, noteId);
	}

	@Test
	@DisplayName("노트 본문이 길면 잘라서 프롬프트에 포함한다")
	void truncatesLongNoteContent() {
		Long userId = 1L;
		Long noteId = 10L;
		String tailMarker = "__TAIL_MARKER__";
		String longContent = "a".repeat(AiPanelService.MAX_LENGTH) + tailMarker;
		AiPanelMessageCommand command = new AiPanelMessageCommand(noteId, "정리해줘");
		NoteDetailResult note = new NoteDetailResult(
				noteId,
				null,
				userId,
				"긴 노트",
				longContent,
				NoteVisibility.PRIVATE,
				false,
				null,
				null
		);
		given(noteService.getNote(userId, noteId)).willReturn(note);
		given(callResponseSpec.content()).willReturn("응답");

		aiPanelService.sendMessage(userId, command);

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(chatClientRequestSpec).user(promptCaptor.capture());
		assertThat(promptCaptor.getValue())
				.contains("a".repeat(AiPanelService.MAX_LENGTH))
				.contains("...(truncated)")
				.doesNotContain(tailMarker);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "   "})
	@DisplayName("노트 본문이 null 또는 blank면 빈 문자열로 문맥에 포함한다")
	void usesEmptyStringWhenNoteContentIsNullOrBlank(String content) {
		Long userId = 1L;
		Long noteId = 10L;
		AiPanelMessageCommand command = new AiPanelMessageCommand(noteId, "정리해줘");
		NoteDetailResult note = new NoteDetailResult(
				noteId,
				null,
				userId,
				"빈 내용 노트",
				content,
				NoteVisibility.PRIVATE,
				false,
				null,
				null
		);
		given(noteService.getNote(userId, noteId)).willReturn(note);
		given(callResponseSpec.content()).willReturn("응답");

		AiPanelMessageResult result = aiPanelService.sendMessage(userId, command);

		ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
		verify(chatClientRequestSpec).user(promptCaptor.capture());
		assertThat(promptCaptor.getValue())
				.contains("빈 내용 노트")
				.contains("정리해줘")
				.doesNotContain("...(truncated)")
				.doesNotContain("null");
		assertThat(result.assistantMessage()).isEqualTo("응답");
		assertThat(result.contextNoteId()).isEqualTo(noteId);
		assertThat(result.contextAttached()).isTrue();
	}

	@Test
	@DisplayName("접근할 수 없는 노트면 모델 호출 전에 예외를 그대로 전달한다")
	void throwsWhenContextNoteIsNotReadable() {
		Long userId = 1L;
		Long noteId = 10L;
		AiPanelMessageCommand command = new AiPanelMessageCommand(noteId, "요약해줘");
		BusinessException exception = new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
		given(noteService.getNote(userId, noteId)).willThrow(exception);

		assertThatThrownBy(() -> aiPanelService.sendMessage(userId, command))
				.isSameAs(exception);
		verify(noteService).getNote(userId, noteId);
		verifyNoInteractions(aiPanelChatClient);
	}
}
