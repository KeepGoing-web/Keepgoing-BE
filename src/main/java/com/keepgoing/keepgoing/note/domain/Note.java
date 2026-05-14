package com.keepgoing.keepgoing.note.domain;

import com.keepgoing.keepgoing.folder.domain.Folder;
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
@Builder(access = AccessLevel.PRIVATE)
public class Note extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private User author;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "folder_id")
	private Folder folder;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false, length = 20)
	private NoteVisibility visibility;

	@Column(name = "ai_collectable", nullable = false)
	@Builder.Default
	private boolean aiCollectable = false;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	// ===== Factory =====

	/**
	 * 글 생성 팩토리 메서드 - visibility가 null이면 기본값 PRIVATE 사용
	 */
	public static Note create(
			User author,
			Folder folder,
			String title,
			String content,
			NoteVisibility visibility,
			Boolean aiCollectable
	) {
		requirePersistedAuthor(author);
		if (folder != null) {
			folder.validateOwner(author.getId());
		}

		if (visibility == NoteVisibility.PUBLIC) {
			validatePublishedContent(title, content);
		}
		validateTitle(title);

		final String finalTitle = (title != null) ? title : "";
		final String finalContent = (content != null) ? content : "";
		final NoteVisibility finalVisibility = (visibility != null) ? visibility : NoteVisibility.PRIVATE;
		final boolean finalAiCollectable = (aiCollectable != null) && aiCollectable;

		return Note.builder()
				.author(author)
				.folder(folder)
				.title(finalTitle)
				.content(finalContent)
				.visibility(finalVisibility)
				.aiCollectable(finalAiCollectable)
				.build();
	}

	private static void validatePublishedContent(String title, String content) {
		if (title == null || title.isBlank() || title.length() > 200) {
			throw new BusinessException(ErrorCode.NOTE_TITLE_INVALID);
		}

		if (content == null || content.isBlank()) {
			throw new BusinessException(ErrorCode.NOTE_CONTENT_REQUIRED);
		}
	}

	/**
	 * 글 내용 수정
	 */
	public void update(
			Long requesterId,
			String title,
			String content,
			NoteVisibility visibility,
			Boolean aiCollectable
	) {
		requireAuthorAccess(requesterId);
		if (visibility == NoteVisibility.PUBLIC) {
			validatePublishedContent(title, content);
		}
		validateTitle(title);

		final String finalTitle = (title != null) ? title : "";
		final String finalContent = (content != null) ? content : "";
		final NoteVisibility finalVisibility = (visibility != null) ? visibility : NoteVisibility.PRIVATE;
		final boolean finalAiCollectable = (aiCollectable != null) && aiCollectable;

		this.title = finalTitle;
		this.content = finalContent;
		this.visibility = finalVisibility;
		this.aiCollectable = finalAiCollectable;
	}

	/**
	 * 제목 변경
	 */
	public void renameTitle(Long requesterId, String newTitle) {
		requireAuthorAccess(requesterId);

		if (newTitle == null || newTitle.isBlank() || newTitle.length() > 200) {
			throw new BusinessException(ErrorCode.NOTE_TITLE_INVALID);
		}
		this.title = newTitle;
	}

	/**
	 * 폴더 변경
	 */
	public void changeFolder(Long requesterId, Folder targetFolder) {
		requireAuthorAccess(requesterId);
		if (targetFolder != null) {
			targetFolder.validateOwner(this.author.getId());
		}

		this.folder = targetFolder;
	}

	// ===== Termination =====

	/**
	 * 소프트 삭제
	 */
	public void softDeleteBy(Long requesterId) {
		requireAuthorAccess(requesterId);
		if (this.deletedAt == null) {
			this.deletedAt = LocalDateTime.now();
		}
	}

	// ===== Queries =====

	/**
	 * 삭제 처리된 게시글인지 여부
	 */
	public boolean isDeleted() {
		return deletedAt != null;
	}

	/**
	 * 이 글의 작성자 여부
	 */
	public boolean isAuthor(Long authorId) {
		return this.author != null
				&& this.author.getId() != null
				&& this.author.getId().equals(authorId);
	}

	/**
	 * 작성자 ID 가져오기
	 */
	public Long getAuthorId() {
		requirePersistedAuthor(this.author);
		return this.author.getId();
	}

	// ===== Internal Guards =====

	private void requireAuthorAccess(Long requesterId) {
		if (!isAuthor(requesterId)) {
			throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
		}
	}

	private static void requirePersistedAuthor(User author) {
		if (author == null || author.getId() == null) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}
	}

	private static void validateTitle(String title) {
		if (title != null && title.length() > 200) {
			throw new BusinessException(ErrorCode.NOTE_TITLE_INVALID);
		}
	}

	public void validateOwner(Long userId) {
		if (!isAuthor(userId)) {
			throw new BusinessException(ErrorCode.NOTE_ACCESS_DENIED);
		}
	}
}
