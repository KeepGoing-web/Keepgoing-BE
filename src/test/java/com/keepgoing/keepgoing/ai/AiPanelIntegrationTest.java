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
import com.keepgoing.keepgoing.note.domain.Note;
import com.keepgoing.keepgoing.note.domain.NoteVisibility;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
					.andExpect(jsonPath("$.data.contextAttached").value(true));

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
}
