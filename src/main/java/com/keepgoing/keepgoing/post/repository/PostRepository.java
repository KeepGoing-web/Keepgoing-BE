package com.keepgoing.keepgoing.post.repository;

import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.domain.PostVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @EntityGraph(attributePaths = {"author"})
    Optional<Post> findById(Long id);

    /**
     * authorId 로 필터해서, 전달받은 Pageable 조건(페이지/정렬)에 맞게 조회한다.
     */
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByAuthor_Id(Long authorId, Pageable pageable);

    /**
     * visibility 값으로 필터해서, createdAt 기준 내림차순으로 정렬해서 찾는다.
     */
    @EntityGraph(attributePaths = {"author"})
    List<Post> findByVisibilityOrderByCreatedAtDesc(PostVisibility visibility);

    // 전체 검색(derived query)
    @EntityGraph(attributePaths = {"author"})
    Page<Post> findByTitleContainingOrContentContaining(
            String titleKeyword,
            String contentKeyword,
            Pageable pageable
    );

    // 내 글 검색(작성자 조건 포함) Before: LIKE 검색
    @Query("""
            SELECT p
            FROM Post p
            WHERE p.author.id = :authorId
              AND (p.title LIKE CONCAT('%', :keyword, '%')
                   OR p.content LIKE CONCAT('%', :keyword, '%'))
            """)
    Page<Post> searchMyPostsLike(@Param("authorId") Long authorId,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    // 내 글 검색(작성자 조건 포함) After: Fulltext 검색
    @Query(
            value = """
                SELECT p.*
                FROM posts p
                WHERE p.author_id = :authorId
                    AND p.deleted_at IS NULL
                    AND MATCH(p.title, p.content) AGAINST (:keyword IN BOOLEAN MODE)
                ORDER BY MATCH(p.title, p.content) AGAINST (:keyword IN BOOLEAN MODE) DESC,
                        p.created_at DESC
    """,
            countQuery = """
                SELECT COUNT(*)
                FROM posts p
                WHERE p.author_id = :authorId
                    AND p.deleted_at IS NULL
                    AND MATCH(p.title, p.content) AGAINST (:keyword IN BOOLEAN MODE)
    """,
            nativeQuery = true
    )
    Page<Post> searchMyPostsFullText(@Param("authorId") Long authorId,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}
