package com.keepgoing.keepgoing.note.service.dto;

import com.keepgoing.keepgoing.note.domain.NoteVisibility;

/**
 * 노트 생성에 필요한 서비스 계층 전용 커맨드입니다.
 *
 * @param userId        작성자 ID
 * @param title         노트 제목
 * @param content       노트 본문 내용
 * @param visibility    공개 범위
 * @param aiCollectable AI 학습/수집에 활용 가능한지 여부
 */
public record NoteCreateCommand(
		Long userId,
		String title,
		String content,
		NoteVisibility visibility,
		boolean aiCollectable
) {
}
