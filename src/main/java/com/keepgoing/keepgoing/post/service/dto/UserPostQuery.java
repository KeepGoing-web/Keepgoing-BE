package com.keepgoing.keepgoing.post.service.dto;

import org.springframework.data.domain.Pageable;

public record UserPostQuery(
        Long authorId,
        Pageable pageable
) {}
