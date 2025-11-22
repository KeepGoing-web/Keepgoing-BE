package com.keepgoing.keepgoing.post.repository;

import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import com.keepgoing.keepgoing.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // @Where 덕분에 deleted_at IS NULL은 자동으로 붙음
    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<Post> findByVisibilityOrderByCreatedAtDesc(PostVisibility visibility);
}
