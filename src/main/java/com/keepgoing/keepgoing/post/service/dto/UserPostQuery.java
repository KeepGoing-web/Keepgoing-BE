package com.keepgoing.keepgoing.post.service.dto;

import org.springframework.data.domain.Pageable;

/**
 * 특정 사용자의 게시글을 조회하기 위한 조건 값들을 담는 조회용 DTO입니다.
 *
 * @param authorId 조회할 사용자 ID
 * @param pageable 페이지 번호, 크기, 정렬 정보를 담는 Pageable
 */
public record UserPostQuery(
        Long authorId,
        Pageable pageable
) {}
