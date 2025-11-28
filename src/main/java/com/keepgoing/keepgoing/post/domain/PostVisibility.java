package com.keepgoing.keepgoing.post.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 블로그 공개 범위
 * - PUBLIC   : 모두에게 공개
 * - PRIVATE  : 작성자만
 * - UNLISTED : 링크 가진 사람만 접근 가능
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public enum PostVisibility {

    PUBLIC("모두에게 공개"),
    PRIVATE("작성자만"),
    UNLISTED("링크 가진 사람만 접근 가능");

    private final String description;
}
