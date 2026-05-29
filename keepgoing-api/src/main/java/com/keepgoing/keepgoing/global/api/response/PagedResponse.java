package com.keepgoing.keepgoing.global.api.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이지네이션 응답")
@JsonPropertyOrder({"contents", "page", "size", "totalElements", "totalPages", "last"})
@Getter
@Builder
@AllArgsConstructor
public class PagedResponse<T> {

    @Schema(description = "현재 페이지의 데이터 목록") private final List<T> contents;

    @Schema(description = "현재 페이지 번호 (0부터 시작)")
    private final int page;

    @Schema(description = "페이지 크기")
    private final int size;

    @Schema(description = "전체 요소 개수")
    private final long totalElements;

    @Schema(description = "전체 페이지 수")
    private final int totalPages;

    @Schema(description = "마지막 페이지 여부")
    private final boolean last;

    public static <T> PagedResponse<T> of(Page<?> pageInfo, List<T> contents) {
        return PagedResponse.<T>builder()
                .contents(contents)
                .page(pageInfo.getNumber())
                .size(pageInfo.getSize())
                .totalElements(pageInfo.getTotalElements())
                .totalPages(pageInfo.getTotalPages())
                .last(pageInfo.isLast())
                .build();
    }
}
