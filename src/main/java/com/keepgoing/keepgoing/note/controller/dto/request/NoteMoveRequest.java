package com.keepgoing.keepgoing.note.controller.dto.request;

import com.keepgoing.keepgoing.global.validation.PositiveIfPresent;
import com.keepgoing.keepgoing.global.validation.PresentJsonNullable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.valueextraction.Unwrapping;
import com.keepgoing.keepgoing.note.service.dto.NoteMoveCommand;
import org.openapitools.jackson.nullable.JsonNullable;

@Schema(description = "노트 이동 요청")
public record NoteMoveRequest(
		@Schema(
				description = "이동할 대상 폴더 ID. null이면 루트로 이동합니다. 필드를 생략하면 validation 에러가 발생합니다.",
				example = "10",
				nullable = true
		)
		@PresentJsonNullable(payload = {Unwrapping.Skip.class})
		@PositiveIfPresent(payload = {Unwrapping.Skip.class})
		JsonNullable<Long> folderId
) {

	public NoteMoveCommand toCommand(Long noteId, Long userId) {
		return new NoteMoveCommand(
				noteId,
				userId,
				this.folderId.orElse(null)
		);
	}
}
