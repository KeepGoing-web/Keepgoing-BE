package com.keepgoing.keepgoing.global.api.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;
import java.util.List;

@Schema(description = "슬라이스 기반 페이지네이션 응답")
@JsonPropertyOrder({"contents", "page", "size", "hasNext"})
@Getter
@Builder
@AllArgsConstructor
public class SliceResponse<T> {

	@Schema(description = "현재 슬라이스의 데이터 목록")
	private final List<T> contents;

	@Schema(description = "현재 페이지 번호 (0부터 시작)")
	private final int page;

	@Schema(description = "페이지 크기")
	private final int size;

	@Schema(description = "다음 페이지 존재 여부")
	private final boolean hasNext;

	public static <T> SliceResponse<T> of(Slice<?> sliceInfo, List<T> contents) {
		return SliceResponse.<T>builder()
				.contents(contents)
				.page(sliceInfo.getNumber())
				.size(sliceInfo.getSize())
				.hasNext(sliceInfo.hasNext())
				.build();
	}
}
