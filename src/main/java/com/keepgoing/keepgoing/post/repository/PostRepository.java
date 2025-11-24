package com.keepgoing.keepgoing.post.repository;

import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * authorId 로 필터해서, createdAt 기준 내림차순으로 정렬해서 찾는다.
     * */
    List<Post> findByAuthor_IdOrderByCreatedAtDesc(Long authorId);

    /**
     * visibility 값으로 필터해서, createdAt 기준 내림차순으로 정렬해서 찾는다.
     * */
    List<Post> findByVisibilityOrderByCreatedAtDesc(PostVisibility visibility);
}
