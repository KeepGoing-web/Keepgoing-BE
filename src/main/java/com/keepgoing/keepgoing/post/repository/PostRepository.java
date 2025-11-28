package com.keepgoing.keepgoing.post.repository;

import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Optional<Post> findById(Long id);

    /**
     * authorId 로 필터해서, 전달받은 Pageable 조건(페이지/정렬)에 맞게 조회한다.
     * */
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByAuthor_Id(Long authorId, Pageable pageable);

    /**
     * visibility 값으로 필터해서, createdAt 기준 내림차순으로 정렬해서 찾는다.
     * */
    @EntityGraph(attributePaths = {"author"})
    List<Post> findByVisibilityOrderByCreatedAtDesc(PostVisibility visibility);
}
