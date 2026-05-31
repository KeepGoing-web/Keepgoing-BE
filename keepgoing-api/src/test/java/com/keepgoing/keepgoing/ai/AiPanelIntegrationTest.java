package com.keepgoing.keepgoing.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.ai.controller.dto.AiPanelMessageRequest;
import com.keepgoing.keepgoing.ai.domain.AiNoteChunk;
import com.keepgoing.keepgoing.ai.domain.AiNoteIndex;
import com.keepgoing.keepgoing.ai.repository.AiNoteChunkRepository;
import com.keepgoing.keepgoing.ai.repository.AiNoteIndexRepository;
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class AiPanelIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	UserRepository userRepository;

	@Autowired
	NoteRepository noteRepository;

	@Autowired
	AiNoteIndexRepository aiNoteIndexRepository;

	@Autowired
	AiNoteChunkRepository aiNoteChunkRepository;

	@Autowired
	EntityManager entityManager;

	@MockitoBean
	ChatClient aiPanelChatClient;

	private ChatClient.ChatClientRequestSpec chatClientRequestSpec;
	private ChatClient.CallResponseSpec callResponseSpec;

	@BeforeEach
	void setUp() {
		chatClientRequestSpec = mock(ChatClient.ChatClientRequestSpec.class);
		callResponseSpec = mock(ChatClient.CallResponseSpec.class);

		lenient().when(aiPanelChatClient.prompt()).thenReturn(chatClientRequestSpec);
		lenient().when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
		lenient().when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("POST /api/ai/panel/messages 통합")
	class SendMessage {

		@Test
		@DisplayName("context note가 있으면 실제 노트 조회/예외 처리 경로를 타고 응답을 반환하며 노트는 수정하지 않는다")
		void sendsMessageWithNoteContextAndKeepsNoteUnchanged() throws Exception {
			User author = saveUser("author@test.com", "작성자");
			Note note = saveNote(author, "회의록", "오늘 논의한 내용");
			mockLoginUser(author.getId());
			given(callResponseSpec.content()).willReturn("AI 응답");

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(APPLICATION_JSON)
							.content(requestJson(new AiPanelMessageRequest(note.getId(), "이 노트를 요약해줘"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.assistantMessage").value("AI 응답"))
					.andExpect(jsonPath("$.data.contextNoteId").value(note.getId()))
					.andExpect(jsonPath("$.data.contextAttached").value(true))
					.andExpect(jsonPath("$.data.citations[0].noteId").value(note.getId()))
					.andExpect(jsonPath("$.data.citations[0].title").value("회의록"))
					.andExpect(jsonPath("$.data.citations[0].excerpt").value("오늘 논의한 내용"))
					.andExpect(jsonPath("$.data.citations[0].sourceType").value("CONTEXT_NOTE"));

			ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
			verify(chatClientRequestSpec).user(promptCaptor.capture());
			assertThat(promptCaptor.getValue())
					.contains("회의록")
					.contains("오늘 논의한 내용")
					.contains("이 노트를 요약해줘");

			entityManager.flush();
			entityManager.clear();

			Note reloaded = noteRepository.findById(note.getId()).orElseThrow();
			assertThat(reloaded.getTitle()).isEqualTo("회의록");
			assertThat(reloaded.getContent()).isEqualTo("오늘 논의한 내용");
		}

		@Test
		@DisplayName("접근할 수 없는 private note를 context로 보내면 NOTE_ACCESS_DENIED를 반환하고 모델은 호출하지 않는다")
		void returnsForbiddenWhenContextNoteIsNotReadable() throws Exception {
			User author = saveUser("author@test.com", "작성자");
			User requester = saveUser("requester@test.com", "요청자");
			Note note = saveNote(author, "작성자 노트", "비공개 내용");
			mockLoginUser(requester.getId());

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(APPLICATION_JSON)
							.content(requestJson(new AiPanelMessageRequest(note.getId(), "요약해줘"))))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("NOTE_ACCESS_DENIED"));

			verifyNoInteractions(aiPanelChatClient);
		}

		@Test
		@DisplayName("message 길이가 최대 제한을 초과하면 400 검증 오류를 반환하고 모델은 호출하지 않는다")
		void returnsBadRequestWhenMessageExceedsLimit() throws Exception {
			String tooLongMessage = "a".repeat(AiPanelMessageRequest.MAX_MESSAGE_LENGTH + 1);

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(APPLICATION_JSON)
							.content(requestJson(new AiPanelMessageRequest(null, tooLongMessage))))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("message"));

			verifyNoInteractions(aiPanelChatClient);
		}

		@Test
		@DisplayName("retrieval 대상 note가 있으면 prompt와 citation 응답에 RETRIEVED_NOTE로 포함한다")
		void returnsRetrievedNoteCitation() throws Exception {
			// given
			User author = saveUser("author-retrieval@test.com", "작성자");
			Note note = saveCollectableNote(author, "배포 회의", "금요일 배포 결정");

			saveCompletedAiIndexAndChunk(
					note,
					"배포 회의",
					"금요일 배포 결정"
			);

			mockLoginUser(author.getId());
			given(callResponseSpec.content()).willReturn("배포 일정 응답");

			// when & then
			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(APPLICATION_JSON)
							.content(requestJson(new AiPanelMessageRequest(null, "배포"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.assistantMessage").value("배포 일정 응답"))
					.andExpect(jsonPath("$.data.contextNoteId").doesNotHaveJsonPath())
					.andExpect(jsonPath("$.data.contextAttached").value(false))
					.andExpect(jsonPath("$.data.citations[0].noteId").value(note.getId()))
					.andExpect(jsonPath("$.data.citations[0].title").value("배포 회의"))
					.andExpect(jsonPath("$.data.citations[0].excerpt").value("금요일 배포 결정"))
					.andExpect(jsonPath("$.data.citations[0].sourceType").value("RETRIEVED_NOTE"));

			ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
			verify(chatClientRequestSpec).user(promptCaptor.capture());

			assertThat(promptCaptor.getValue())
					.contains("[검색된 관련 노트]")
					.contains("배포 회의")
					.contains("금요일 배포 결정")
					.contains("[답변 규칙]")
					.contains("노트에서 확인되지 않습니다")
					.contains("배포");
		}

		@Test
		@DisplayName("다른 사용자의 indexed note는 retrieval prompt와 citation에 포함하지 않는다")
		void doesNotRetrieveOtherUsersNote() throws Exception {
			// given
			User author = saveUser("author-private@test.com", "작성자");
			User requester = saveUser("requester@test.com", "요청자");

			Note otherUserNote = saveCollectableNote(author, "배포 회의", "다른 사용자의 배포 내용");

			saveCompletedAiIndexAndChunk(
					otherUserNote,
					"배포 회의",
					"다른 사용자의 배포 내용"
			);

			mockLoginUser(requester.getId());
			given(callResponseSpec.content()).willReturn("근거 없음 응답");

			// when & then
			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(APPLICATION_JSON)
							.content(requestJson(new AiPanelMessageRequest(null, "배포"))))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.assistantMessage").value("근거 없음 응답"))
					.andExpect(jsonPath("$.data.contextAttached").value(false))
					.andExpect(jsonPath("$.data.citations").isEmpty());

			ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
			verify(chatClientRequestSpec).user(promptCaptor.capture());

			assertThat(promptCaptor.getValue())
					.doesNotContain("다른 사용자의 배포 내용")
					.doesNotContain("[검색된 관련 노트]")
					.contains("[답변 규칙]");
		}
	}

	private User saveUser(String email, String name) {
		return userRepository.saveAndFlush(User.create(email, name));
	}

	private Note saveNote(User author, String title, String content) {
		Note note = Note.create(author, null, title, content, NoteVisibility.PRIVATE, false);
		return noteRepository.saveAndFlush(note);
	}

	private String requestJson(Object request) throws Exception {
		return objectMapper.writeValueAsString(request);
	}

	private void mockLoginUser(Long userId) {
		Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
				userId,
				null,
				List.of(new SimpleGrantedAuthority(UserRole.USER.toAuthority()))
		);
		SecurityContextHolder.getContext().setAuthentication(authenticated);
	}

	private Note saveCollectableNote(User author, String title, String content) {
		Note note = Note.create(author, null, title, content, NoteVisibility.PRIVATE, true);
		return noteRepository.saveAndFlush(note);
	}

	private void saveCompletedAiIndexAndChunk(
			Note note,
			String title,
			String contentChunk
	) {
		LocalDateTime requestedAt = LocalDateTime.of(2026, 5, 28, 10, 0);
		LocalDateTime indexedAt = requestedAt.plusMinutes(1);

		AiNoteIndex index = AiNoteIndex.pending(
				note.getId(),
				note.getAuthor().getId(),
				requestedAt
		);
		index.markCompleted(indexedAt);
		aiNoteIndexRepository.save(index);

		aiNoteChunkRepository.save(AiNoteChunk.create(
				note.getId(),
				note.getAuthor().getId(),
				0,
				title,
				contentChunk,
				note.getUpdatedAt(),
				indexedAt
		));

		entityManager.flush();
		entityManager.clear();
	}
}
