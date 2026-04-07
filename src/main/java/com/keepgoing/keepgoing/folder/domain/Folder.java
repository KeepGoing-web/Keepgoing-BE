package com.keepgoing.keepgoing.folder.domain;

import com.keepgoing.keepgoing.global.common.entity.BaseEntity;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "folders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Folder extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id", nullable = true)
	private Folder parent;

	@Column(name = "name", nullable = false, length = 120)
	private String name;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	public static Folder create(User owner, Folder parent, String name) {
		if (owner == null || owner.getId() == null) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}

		if (parent != null) {
			parent.validateOwner(owner.getId());
		}

		validateName(name);

		return new Folder(
				null,
				owner,
				parent,
				name,
				null
		);
	}

	private static void validateName(String name) {
		if (name == null || name.isBlank() || name.contains("/")) {
			throw new BusinessException(ErrorCode.FOLDER_INVALID_NAME);
		}
	}

	public void rename(String name) {
		validateName(name);
		this.name = name;
	}

	public void moveTo(Folder parent) {
		this.parent = parent;
	}

	public void softDelete() {
		if (this.deletedAt == null) {
			this.deletedAt = LocalDateTime.now();
		}
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public void validateOwner(Long userId) {
		if (owner == null || owner.getId() == null || !owner.getId().equals(userId)) {
			throw new BusinessException(ErrorCode.FOLDER_ACCESS_DENIED);
		}
	}
}
