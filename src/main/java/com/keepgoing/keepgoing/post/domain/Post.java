package com.keepgoing.keepgoing.post.domain;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@EntityListeners(AuditingEntityListener.class)
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content",nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility",nullable = false, length = 20)
    @Builder.Default
    private PostVisibility visibility = PostVisibility.PRIVATE;

    @Column(name = "ai_collectable", nullable = false)
    @Builder.Default
    private boolean aiCollectable = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // === 비즈니스 로직 ===
    /**
     * 글 생성 팩토리 메서드
     * - visibility가 null이면 기본값 PRIVATE 사용
     */
    public static Post create(User author,
                              String title,
                              String content,
                              PostVisibility visibility,
                              boolean aiCollectable) {

        PostVisibility finalVisibility =
                (visibility != null) ? visibility : PostVisibility.PRIVATE;

        return Post.builder()
                .author(author)
                .title(title)
                .content(content)
                .visibility(finalVisibility)
                .aiCollectable(aiCollectable)
                .build();
    }

    /**
     * 글 내용 수정
     */
    public void update(String title,
                       String content,
                       PostVisibility visibility,
                       boolean aiCollectable) {
        this.title = title;
        this.content = content;
        this.visibility = visibility;
        this.aiCollectable = aiCollectable;
    }

    /**
     * 소프트 삭제
     */
    public void softDelete() {
        if (this.deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }

    /**
     * 삭제 여부 (테스트/관리용)
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 작성자 권한 검증
     */
    public void validateAuthor(Long authorID) {
        if (!isAuthor(authorID)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }
    }

    /**
     * 이 글의 작성자 여부
     * */
    public boolean isAuthor(Long authorId) {
        return this.author != null
                && this.author.getId() != null
                && this.author.getId().equals(authorId);
    }

    /**
     * 작성자 ID 가져오기
     */
    public Long getAuthorId() {
        if (this.author == null || this.author.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return this.author.getId();
    }
}
