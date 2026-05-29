package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageCommand;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageResult;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;


/**
 * 초기 AI 패널 메시지 채널 서비스.
 *
 * 현재 구현은 사용자의 메시지를 받아 GLM 응답을 반환하는 최소 채널에 집중한다.
 *
 * note context가 주어지면 기존 NoteService의 읽기 권한 규칙을 재사용해
 * 현재 노트 문맥을 프롬프트에 포함한다.
 *
 * 현재 책임은 패널 메시지 orchestration + 모델 호출에 한정한다.
 */
@Service
@RequiredArgsConstructor
public class AiPanelService {

	public static final int MAX_LENGTH = 3000;
	private final ChatClient aiPanelChatClient;

	private final NoteService noteService;

	public AiPanelMessageResult sendMessage(Long userId, AiPanelMessageCommand command) {
		// note context 조회
		AiNoteContext context = loadNoteContextIfPresent(userId, command);

		String userPrompt = buildUserPrompt(command.message(), context);

		String assistantMessage = aiPanelChatClient.prompt()
				.user(userPrompt)
				.call()
				.content();

		return new AiPanelMessageResult(
			assistantMessage,
			context != null ? context.noteId : null,
			context != null
		);
	}

	private AiNoteContext loadNoteContextIfPresent(Long userId, AiPanelMessageCommand command) {
		if (command.contextNoteId() == null) {
			return null;
		}

		// 현재 note에 대한 직접 상호작용은 허용한다.
		// aiCollectable은 이후 retrieval/RAG 계열 기능의 gate로 해석한다.
		NoteDetailResult note = noteService.getNote(userId, command.contextNoteId());
		return AiNoteContext.from(note);
	}

	// TODO:
	// AiActionType별 프롬프트 포맷 분기가 생기면 buildUserPrompt 책임을
	// AiPromptBuilder / AiPromptAssembler로 분리한다.
	// 현재는 단일 메시지 흐름이라 AiPanelService 내부에 유지한다.
	private String buildUserPrompt(String message, AiNoteContext context) {
		if (context == null) {
			return """
					[사용자 메시지]
					%s
					"""
					.formatted(message);
		}

		return """
				[현재 노트 문맥]
				- noteId: %s
				- title: %s
				- content:
				%s
				
				[사용자 메시지]
				%s
				"""
				.formatted(
						context.noteId(),
						context.title(),
						truncate(context.content()),
						message
				);
	}

	private String truncate(String content) {
		if (content == null || content.isBlank()) {
			return "";
		}

		if (content.length() <= MAX_LENGTH) {
			return content;
		}
		return content.substring(0, MAX_LENGTH) + "\n...(truncated)";
	}


	// AI가 실제로 사용하는 현재 노트 문맥의 최소 표현.
	// NoteDetailResult 전체를 AI 로직에 퍼뜨리지 않기 위해
	// noteId/title/content만 남긴 최소 모델로 좁혀서 사용한다.
	// 임시로 사용
	private record AiNoteContext(
			Long noteId,
			String title,
			String content
	) {
		public static AiNoteContext from(NoteDetailResult result) {
			return new AiNoteContext(
					result.noteId(),
					result.title(),
					result.content()
			);
		}
	}
}
