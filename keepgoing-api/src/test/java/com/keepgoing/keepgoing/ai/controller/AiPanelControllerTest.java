package com.keepgoing.keepgoing.ai.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepgoing.keepgoing.ai.controller.dto.AiPanelMessageRequest;
import com.keepgoing.keepgoing.ai.service.AiPanelService;
import com.keepgoing.keepgoing.ai.service.dto.AiCitationSourceType;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelCitationResult;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageCommand;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageResult;
import com.keepgoing.keepgoing.global.api.exception.GlobalExceptionHandler;
import com.keepgoing.keepgoing.global.config.JacksonConfig;
import com.keepgoing.keepgoing.global.security.cookie.CookieConfig;
import com.keepgoing.keepgoing.global.security.jwt.JwtProvider;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AiPanelController.class)
@Import({GlobalExceptionHandler.class, CookieConfig.class, JacksonConfig.class})
class AiPanelControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	AiPanelService aiPanelService;

	@MockitoBean
	JpaMetamodelMappingContext jpaMappingContext;

	@MockitoBean
	JwtProvider jwtProvider;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Nested
	@DisplayName("POST /api/ai/panel/messages")
	class ChatAi {

		@Test
		@DisplayName("인증 사용자 요청이면 메시지 커맨드를 서비스에 전달하고 응답을 반환한다")
		void success() throws Exception {
			Long userId = 1L;
			AiPanelMessageRequest request = new AiPanelMessageRequest(10L, "요약해줘");
			AiPanelMessageResult result = new AiPanelMessageResult(
					"정리된 응답",
					10L,
					true,
					List.of(new AiPanelCitationResult(
							10L,
							"회의록",
							"회의 내용",
							AiCitationSourceType.CONTEXT_NOTE
					))
			);

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			given(aiPanelService.sendMessage(eq(userId), any(AiPanelMessageCommand.class)))
					.willReturn(result);

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.assistantMessage").value("정리된 응답"))
					.andExpect(jsonPath("$.data.contextNoteId").value(10L))
					.andExpect(jsonPath("$.data.contextAttached").value(true))
					.andExpect(jsonPath("$.data.citations[0].noteId").value(10L))
					.andExpect(jsonPath("$.data.citations[0].title").value("회의록"))
					.andExpect(jsonPath("$.data.citations[0].excerpt").value("회의 내용"))
					.andExpect(jsonPath("$.data.citations[0].sourceType").value("CONTEXT_NOTE"));

			ArgumentCaptor<AiPanelMessageCommand> captor = ArgumentCaptor.forClass(AiPanelMessageCommand.class);
			verify(aiPanelService).sendMessage(eq(userId), captor.capture());
			assertThat(captor.getValue().contextNoteId()).isEqualTo(10L);
			assertThat(captor.getValue().message()).isEqualTo("요약해줘");
		}

		@Test
		@DisplayName("contextNoteId가 없으면 null 문맥 상태로 응답한다")
		void successWithoutContextNoteId() throws Exception {
			Long userId = 1L;
			AiPanelMessageRequest request = new AiPanelMessageRequest(null, "그냥 답해줘");
			AiPanelMessageResult result = new AiPanelMessageResult("일반 응답", null, false, List.of());

			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			given(aiPanelService.sendMessage(eq(userId), any(AiPanelMessageCommand.class)))
					.willReturn(result);

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.assistantMessage").value("일반 응답"))
					.andExpect(jsonPath("$.data.contextNoteId").doesNotHaveJsonPath())
					.andExpect(jsonPath("$.data.contextAttached").value(false))
					.andExpect(jsonPath("$.data.citations").isEmpty());

			ArgumentCaptor<AiPanelMessageCommand> captor = ArgumentCaptor.forClass(AiPanelMessageCommand.class);
			verify(aiPanelService).sendMessage(eq(userId), captor.capture());
			assertThat(captor.getValue().contextNoteId()).isNull();
			assertThat(captor.getValue().message()).isEqualTo("그냥 답해줘");
		}

		@Test
		@DisplayName("message가 비어 있으면 validation 에러를 반환한다")
		void returnsBadRequestWhenMessageIsBlank() throws Exception {
			AiPanelMessageRequest request = new AiPanelMessageRequest(null, " ");

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("message"));
		}

		@Test
		@DisplayName("message 필드가 없으면 validation 에러를 반환한다")
		void returnsBadRequestWhenMessageIsMissing() throws Exception {
			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "contextNoteId": 10
									}
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("message"));
		}

		@Test
		@DisplayName("message가 최대 길이를 초과하면 validation 에러를 반환하고 서비스를 호출하지 않는다")
		void returnsBadRequestWhenMessageExceedsLimit() throws Exception {
			AiPanelMessageRequest request = new AiPanelMessageRequest(
					null,
					"a".repeat(AiPanelMessageRequest.MAX_MESSAGE_LENGTH + 1)
			);

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
					.andExpect(jsonPath("$.error.fieldErrors[0].field").value("message"));

			verifyNoInteractions(aiPanelService);
		}

		@Test
		@DisplayName("일시적 AI 예외가 발생하면 503과 재시도 안내 메시지를 반환한다")
		void returnsServiceUnavailableWhenTransientAiExceptionOccurs() throws Exception {
			Long userId = 1L;
			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			given(aiPanelService.sendMessage(eq(userId), any(AiPanelMessageCommand.class)))
					.willThrow(new TransientAiException("retry exhausted"));

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "message": "요약해줘"
									}
									"""))
					.andExpect(status().isServiceUnavailable())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
					.andExpect(jsonPath("$.error.message").value("AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요."));
		}

		@Test
		@DisplayName("비일시적 AI 예외가 발생하면 503과 일반 실패 메시지를 반환한다")
		void returnsServiceUnavailableWhenNonTransientAiExceptionOccurs() throws Exception {
			Long userId = 1L;
			SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

			given(aiPanelService.sendMessage(eq(userId), any(AiPanelMessageCommand.class)))
					.willThrow(new NonTransientAiException("bad request"));

			mockMvc.perform(post("/api/ai/panel/messages")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "message": "요약해줘"
									}
									"""))
					.andExpect(status().isServiceUnavailable())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
					.andExpect(jsonPath("$.error.message").value("AI 서비스 요청이 실패했습니다."));
		}
	}
}
