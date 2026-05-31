package com.keepgoing.keepgoing.ai.service;

import com.keepgoing.keepgoing.ai.service.dto.AiCitationSourceType;
import com.keepgoing.keepgoing.ai.service.dto.AiNoteRetrievalResult;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelCitationResult;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageCommand;
import com.keepgoing.keepgoing.ai.service.dto.AiPanelMessageResult;
import com.keepgoing.keepgoing.note.service.NoteService;
import com.keepgoing.keepgoing.note.service.dto.NoteDetailResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;


/**
 * 초기 AI 패널 메시지 채널 서비스.
 * <p>
 * 현재 구현은 사용자의 메시지를 받아 GLM 응답을 반환하는 최소 채널에 집중한다.
 * <p>
 * note context가 주어지면 기존 NoteService의 읽기 권한 규칙을 재사용해 현재 노트 문맥을 프롬프트에 포함한다.
 * <p>
 * 현재 책임은 패널 메시지 orchestration + 모델 호출에 한정한다.
 */
@Service
@RequiredArgsConstructor
public class AiPanelService {

	public static final int MAX_LENGTH = 3000;
	private final ChatClient aiPanelChatClient;

	private final NoteService noteService;
	private final AiNoteRetrievalService aiNoteRetrievalService;

	public AiPanelMessageResult sendMessage(Long userId, AiPanelMessageCommand command) {
		// note context 조회
		AiPanelContextNote context = loadNoteContextIfPresent(userId, command);

		List<AiNoteRetrievalResult> retrievedNotes = aiNoteRetrievalService.retrieve(
				userId,
				command.message(),
				context != null ? context.noteId() : null
		);

		String userPrompt = buildUserPrompt(command.message(), context, retrievedNotes);

		String assistantMessage = aiPanelChatClient.prompt()
				.user(userPrompt)
				.call()
				.content();

		List<AiPanelCitationResult> citations = buildCitations(context, retrievedNotes);

		return new AiPanelMessageResult(
				assistantMessage,
				context != null ? context.noteId() : null,
				context != null,
				citations
		);
	}

	private AiPanelContextNote loadNoteContextIfPresent(Long userId, AiPanelMessageCommand command) {
		if (command.contextNoteId() == null) {
			return null;
		}

		// 현재 note에 대한 직접 상호작용은 허용한다.
		// aiCollectable은 이후 retrieval/RAG 계열 기능의 gate로 해석한다.
		NoteDetailResult note = noteService.getNote(userId, command.contextNoteId());
		return AiPanelContextNote.from(note);
	}

	// TODO:
	// AiActionType별 프롬프트 포맷 분기가 생기면 buildUserPrompt 책임을
	// AiPromptBuilder / AiPromptAssembler로 분리한다.
	// 현재는 단일 메시지 흐름이라 AiPanelService 내부에 유지한다.
	private String buildUserPrompt(
			String message,
			AiPanelContextNote context,
			List<AiNoteRetrievalResult> retrievedNotes
	) {
		StringBuilder prompt = new StringBuilder();

		if (context != null) {
			prompt.append("""
					[현재 노트 문맥]
					- noteId: %s
					- title: %s
					- content:
					%s
					
					""".formatted(
					context.noteId(),
					context.title(),
					truncate(context.content())
			));
		}

		if (!retrievedNotes.isEmpty()) {
			prompt.append("[검색된 관련 노트]\n");

			for (int i = 0; i < retrievedNotes.size(); i++) {
				AiNoteRetrievalResult note = retrievedNotes.get(i);

				prompt.append("""
						%d. noteId: %s
						   title: %s
						   excerpt:
						   %s
						
						""".formatted(
						i + 1,
						note.noteId(),
						note.title(),
						note.excerpt()
				));
			}
		}

		prompt.append("""
				[답변 규칙]
				- 제공된 현재 노트 문맥과 검색된 관련 노트 문맥만 근거로 답변한다.
				- 근거가 부족하면 추측하지 말고 "노트에서 확인되지 않습니다"라고 말한다.
				- 실제 저장/수정/삭제가 완료된 것처럼 말하지 않는다.
				- 참고한 노트가 있다면 답변 내용이 해당 노트 문맥과 연결되도록 답한다.
				
				[사용자 메시지]
				%s
				""".formatted(message));

		return prompt.toString();
	}

	private List<AiPanelCitationResult> buildCitations(
			AiPanelContextNote context,
			List<AiNoteRetrievalResult> retrievedNotes
	) {
		List<AiPanelCitationResult> citations = new ArrayList<>();

		if (context != null) {
			citations.add(new AiPanelCitationResult(
					context.noteId(),
					context.title(),
					truncate(context.content()),
					AiCitationSourceType.CONTEXT_NOTE
			));
		}

		for (AiNoteRetrievalResult note : retrievedNotes) {
			citations.add(new AiPanelCitationResult(
					note.noteId(),
					note.title(),
					note.excerpt(),
					AiCitationSourceType.RETRIEVED_NOTE
			));
		}

		return citations;
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
	private record AiPanelContextNote(
			Long noteId,
			String title,
			String content
	) {
		public static AiPanelContextNote from(NoteDetailResult result) {
			return new AiPanelContextNote(
					result.noteId(),
					result.title(),
					result.content()
			);
		}
	}
}
