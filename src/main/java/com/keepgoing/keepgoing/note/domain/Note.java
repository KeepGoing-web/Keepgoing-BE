package com.keepgoing.keepgoing.note.domain;

import com.keepgoing.keepgoing.global.common.entity.BaseEntity;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "notes")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Note extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    @Builder.Default
    private NoteVisibility visibility = NoteVisibility.PRIVATE;

    @Column(name = "ai_collectable", nullable = false)
    @Builder.Default
    private boolean aiCollectable = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // === 비즈니스 로직 ===
    /**
     * 글 생성 팩토리 메서드
     * - visibility가 null이면 기본값 PRIVATE 사용
     */
    public static Note create(User author,
                              String title,
                              String content,
                              NoteVisibility visibility,
                              Boolean aiCollectable) {

        if (author == null || author.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        NoteVisibility finalVisibility =
                (visibility != null) ? visibility : NoteVisibility.PRIVATE;

        boolean finalAiCollectable =
                (aiCollectable != null) && aiCollectable;

        return Note.builder()
                .author(author)
                .title(title)
                .content(content)
                .visibility(finalVisibility)
                .aiCollectable(finalAiCollectable)
                .build();
    }

    /**
     * 글 내용 수정
     */
    public void update(String title,
                       String content,
                       NoteVisibility visibility,
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
     * 삭제 처리된 게시글인지 여부
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 작성자 권한 검증
     */
    public void validateAuthor(Long authorId) {
        if (!isAuthor(authorId)) {
            throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
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
