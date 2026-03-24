package com.keepgoing.keepgoing.folder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FolderTest {

	@Test
	@DisplayName("부모 폴더가 없으면 루트 폴더를 생성한다")
	void create_createsRootFolderWhenParentIsNull() {
		// given
		User owner = createUser(1L);

		// when
		Folder folder = Folder.create(owner, null, "backend");

		// then
		assertThat(folder.getOwner()).isEqualTo(owner);
		assertThat(folder.getParent()).isNull();
		assertThat(folder.getName()).isEqualTo("backend");
		assertThat(folder.getDeletedAt()).isNull();
	}

	@Test
	@DisplayName("부모 폴더가 있으면 하위 폴더를 생성한다")
	void create_createsChildFolderWhenParentExists() {
		// given
		User owner = createUser(1L);
		Folder parent = Folder.create(owner, null, "root");

		// when
		Folder child = Folder.create(owner, parent, "java");

		// then
		assertThat(child.getParent()).isEqualTo(parent);
		assertThat(child.getName()).isEqualTo("java");
	}

	@Test
	@DisplayName("소유자가 없으면 폴더를 생성할 수 없다")
	void create_throwsWhenOwnerIsNull() {
		assertThatThrownBy(() -> Folder.create(null, null, "backend"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
	}

	@Test
	@DisplayName("소유자 ID가 없으면 폴더를 생성할 수 없다")
	void create_throwsWhenOwnerIdIsNull() {
		// given
		User owner = createUser(null);

		// when & then
		assertThatThrownBy(() -> Folder.create(owner, null, "backend"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
	}

	@Test
	@DisplayName("폴더 이름이 공백이면 폴더를 생성할 수 없다")
	void create_throwsWhenNameIsBlank() {
		// given
		User owner = createUser(1L);

		// when & then
		assertThatThrownBy(() -> Folder.create(owner, null, "   "))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_INVALID_NAME);
	}

	@Test
	@DisplayName("폴더 이름에 슬래시(/)가 포함되면 폴더를 생성할 수 없다")
	void create_throwsWhenNameContainsSlash() {
		// given
		User owner = createUser(1L);

		// when & then
		assertThatThrownBy(() -> Folder.create(owner, null, "back/end"))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_INVALID_NAME);
	}

	@Test
	@DisplayName("폴더 이름을 변경한다")
	void rename_changesFolderName() {
		// given
		User owner = createUser(1L);
		Folder folder = Folder.create(owner, null, "old");

		// when
		folder.rename("new");

		// then
		assertThat(folder.getName()).isEqualTo("new");
	}

	@Test
	@DisplayName("폴더 이름이 공백이면 이름을 변경할 수 없다.")
	void rename_throwsWhenNameIsBlank() {
		// given
		User owner = createUser(1L);
		Folder folder = Folder.create(owner, null, "backend");

		// when & then
		assertThatThrownBy(() -> folder.rename("   "))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_INVALID_NAME);
	}

	@Test
	@DisplayName("소유자가 일치하면 접근 권한 검증을 통과한다")
	void validateOwner_passesWhenOwnerMatches() {
		// given
		User owner = createUser(1L);
		Folder folder = Folder.create(owner, null, "backend");

		// when & then
		assertThatCode(() -> folder.validateOwner(1L))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("소유자가 일치하지 않으면 접근 권한 검증에 실패한다")
	void validateOwner_throwsWhenOwnerDoesNotMatch() {
		User owner = createUser(1L);
		Folder folder = Folder.create(owner, null, "backend");

		assertThatThrownBy(() -> folder.validateOwner(2L))
				.isInstanceOf(BusinessException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
	}

	@Test
	@DisplayName("폴더를 소프트 삭제한다")
	void softDelete_marksFolderAtDeleted() {
		User owner = createUser(1L);
		Folder folder = Folder.create(owner, null, "backend");

		folder.softDelete();

		assertThat(folder.isDeleted()).isTrue();
		assertThat(folder.getDeletedAt()).isNotNull();
	}

	private User createUser(Long id) {
		return User.builder()
				.id(id)
				.email("test@test.com")
				.name("tester")
				.build();
	}
}
