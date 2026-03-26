package com.keepgoing.keepgoing.folder.service;


import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.folder.service.dto.FolderTreeNodeResult;
import com.keepgoing.keepgoing.folder.service.dto.FolderTreeRow;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

	public static final String DIRECTORY_NAME = "backend";
	@Mock
	UserRepository userRepository;

	@Mock
	FolderRepository folderRepository;

	@InjectMocks
	FolderService folderService;

	private User createUser(Long id) {
		return User.builder()
				.id(id)
				.email("test@test.com")
				.name("test")
				.build();
	}

	@Nested
	@DisplayName("createFolder()")
	class CreateFolder {

		@Test
		@DisplayName("부모가 없으면 루트 폴더를 생성한다.")
		void createFolder_createsRootFolder() {
			// given
			Long userId = 1L;
			User user = createUser(userId);
			FolderCreateCommand command = new FolderCreateCommand(userId, null, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.existsRootFolder(userId, DIRECTORY_NAME)).willReturn(false);

			Folder saved = Folder.create(user, null, DIRECTORY_NAME);
			ReflectionTestUtils.setField(saved, "id", 10L);
			given(folderRepository.save(any(Folder.class))).willReturn(saved);

			// when
			FolderSummaryResult result = folderService.createFolder(command);

			// then
			assertThat(result.folderId()).isEqualTo(10L);
			assertThat(result.parentId()).isNull();
			assertThat(result.name()).isEqualTo(DIRECTORY_NAME);

			verify(userRepository).findById(userId);
			verify(folderRepository).existsRootFolder(userId, DIRECTORY_NAME);
			verify(folderRepository).save(any(Folder.class));
			verifyNoMoreInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("부모가 있으면 하위 폴더를 생성한다.")
		void createFolder_createsChildFolder() {
			// given
			Long userId = 1L;
			Long parentId = 2L;
			User user = createUser(userId);
			Folder parent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			FolderCreateCommand command = new FolderCreateCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.findByIdAndDeletedAtIsNull(parentId)).willReturn(Optional.of(parent));
			given(folderRepository.existsChildFolder(userId, parentId, DIRECTORY_NAME)).willReturn(false);

			Folder saved = Folder.create(user, parent, DIRECTORY_NAME);
			ReflectionTestUtils.setField(saved, "id", 11L);
			given(folderRepository.save(any(Folder.class))).willReturn(saved);

			// when
			FolderSummaryResult result = folderService.createFolder(command);

			// then
			assertThat(result.folderId()).isEqualTo(11L);
			assertThat(result.parentId()).isEqualTo(parentId);
			assertThat(result.name()).isEqualTo(DIRECTORY_NAME);

			verify(userRepository).findById(userId);
			verify(folderRepository).findByIdAndDeletedAtIsNull(parentId);
			verify(folderRepository).existsChildFolder(userId, parentId, DIRECTORY_NAME);
			verify(folderRepository).save(any(Folder.class));
			verifyNoMoreInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다.")
		void createFolder_throwsWhenUserNotFound() {
			// given
			Long userId = 1L;
			FolderCreateCommand command = new FolderCreateCommand(userId, null, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

			verify(userRepository).findById(userId);
			verifyNoMoreInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("부모 폴더가 없으면 FOLDER_NOT_FOUND 예외가 발생한다.")
		void createFolder_throwsWhenParentNotFound() {
			// given
			Long userId = 1L;
			Long parentId = 2L;
			User user = createUser(userId);

			FolderCreateCommand command = new FolderCreateCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.findByIdAndDeletedAtIsNull(parentId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);
			verify(userRepository).findById(userId);
			verify(folderRepository).findByIdAndDeletedAtIsNull(parentId);
			verifyNoMoreInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("다른 사용자의 부모 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다.")
		void createFolder_throwsWhenParentOwnedByAnotherUser() {
			// given
			Long userId = 1L;
			Long parentId = 2L;

			User user = createUser(userId);
			User otherUser = createUser(99L);

			Folder parent = Folder.create(otherUser, null, "other-root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			FolderCreateCommand command = new FolderCreateCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.findByIdAndDeletedAtIsNull(parentId)).willReturn(Optional.of(parent));

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);

			verify(userRepository).findById(userId);
			verify(folderRepository).findByIdAndDeletedAtIsNull(parentId);
			verifyNoMoreInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("같은 루트 이름이 이미 있으면 FOLDER_NAME_DUPLICATED 예외가 발생한다.")
		void createFolder_throwsWhenRootFolderNameDuplicated() {
			// given
			Long userId = 1L;
			User user = createUser(userId);

			FolderCreateCommand command = new FolderCreateCommand(userId, null, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.existsRootFolder(userId, DIRECTORY_NAME)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NAME_DUPLICATED);

			verify(userRepository).findById(userId);
			verify(folderRepository).existsRootFolder(userId, DIRECTORY_NAME);
			verifyNoMoreInteractions(userRepository, folderRepository);
		}


		@Test
		@DisplayName("같은 부모 아래 같은 이름이 이미 있으면 FOLDER_NAME_DUPLICATED 예외가 발생한다.")
		void createFolder_throwsWhenChildFolderNameDuplicated() {
			// given
			Long userId = 1L;
			Long parentId = 2L;

			User user = createUser(userId);
			Folder parent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			FolderCreateCommand command = new FolderCreateCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.findByIdAndDeletedAtIsNull(parentId)).willReturn(Optional.of(parent));
			given(folderRepository.existsChildFolder(userId, parentId, DIRECTORY_NAME)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NAME_DUPLICATED);

			verify(userRepository).findById(userId);
			verify(folderRepository).findByIdAndDeletedAtIsNull(parentId);
			verify(folderRepository).existsChildFolder(userId, parentId, DIRECTORY_NAME);
			verifyNoMoreInteractions(userRepository, folderRepository);
		}
	}

	// ========== getFolders ==========
	@Nested
	@DisplayName("getFolders()")
	class GetFolders {

		@Test
		@DisplayName("prentId가 없으면 루트 폴더 목록을 이름순으로 조회한다.")
		void getFolders_returnsRootFoldersOrderedByName() {
			//given
			Long userId = 1L;
			User user = createUser(userId);

			Folder a = Folder.create(user, null, "a");
			ReflectionTestUtils.setField(a, "id", 1L);

			Folder b = Folder.create(user, null, "b");
			ReflectionTestUtils.setField(b, "id", 2L);

			given(folderRepository.findRootFolders(userId)).willReturn(List.of(a, b));

			//when
			List<FolderSummaryResult> results = folderService.getFolders(userId, null);

			//then
			assertThat(results).hasSize(2);
			assertThat(results.get(0).name()).isEqualTo("a");
			assertThat(results.get(1).name()).isEqualTo("b");
			assertThat(results.get(0).parentId()).isNull();
			assertThat(results.get(1).parentId()).isNull();

			verify(folderRepository).findRootFolders(userId);
			verifyNoMoreInteractions(folderRepository);
		}

		@Test
		@DisplayName("parentId가 있으면 부모 소유권을 검증한 뒤 자식 폴더 목록을 이름순으로 조회한다.")
		void getFolders_returnsChildFoldersAfterOwnerValidation() {
			//given
			Long userId = 1L;
			Long parentId = 10L;
			User user = createUser(userId);

			Folder parent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			Folder child1 = Folder.create(user, parent, "backend");
			ReflectionTestUtils.setField(child1, "id", 11L);
			Folder child2 = Folder.create(user, parent, "frontend");
			ReflectionTestUtils.setField(child2, "id", 12L);

			given(folderRepository.findByIdAndDeletedAtIsNull(parentId)).willReturn(Optional.of(parent));
			given(folderRepository.findChildFolders(userId, parentId)).willReturn(List.of(child1, child2));

			//when
			List<FolderSummaryResult> results = folderService.getFolders(userId, parentId);

			//then
			assertThat(results).hasSize(2);
			assertThat(results.get(0).parentId()).isEqualTo(parentId);
			assertThat(results.get(1).parentId()).isEqualTo(parentId);
			assertThat(results.get(0).name()).isEqualTo("backend");
			assertThat(results.get(1).name()).isEqualTo("frontend");

			verify(folderRepository).findByIdAndDeletedAtIsNull(parentId);
			verify(folderRepository).findChildFolders(userId, parentId);
			verifyNoMoreInteractions(folderRepository);
		}

		@Test
		@DisplayName("parentId가 있는데 부모 폴더가 없으면 FOLDER_NOT_FOUND 예외가 발생한다.")
		void getFolders_throwsWhenParentNotFound() {
			//given
			Long userId = 1L;
			Long parentId = 10L;

			given(folderRepository.findByIdAndDeletedAtIsNull(parentId)).willReturn(Optional.empty());

			//when & then
			assertThatThrownBy(() -> folderService.getFolders(userId, parentId)).isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);

			verify(folderRepository).findByIdAndDeletedAtIsNull(parentId);
			verifyNoMoreInteractions(folderRepository);
		}

		@Test
		@DisplayName("다른 사용자의 부모 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다.")
		void getFolders_throwsWhenParentOwnedByAnotherUser() {
			//given
			Long userId = 1L;
			Long parentId = 10L;

			User other = createUser(99L);
			Folder parent = Folder.create(other, null, "other-root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(parentId)).willReturn(Optional.of(parent));

			//when & then
			assertThatThrownBy(() -> folderService.getFolders(userId, parentId)).isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);

			verify(folderRepository).findByIdAndDeletedAtIsNull(parentId);
			verifyNoMoreInteractions(folderRepository);
		}
	}
	@Nested
	@DisplayName("getFolderTree()")
	class GetFolderTree {

		@Test
		@DisplayName("전체 폴더를 1번 조회해서 parentId 기반 트리 구조로 조립한다.")
		void getFolderTree_buildsTreeFromRows() {
			// given
			Long userId = 1L;

			given(folderRepository.findTreeRows(userId)).willReturn(List.of(
					new FolderTreeRow(1L, null, "공부"),
					new FolderTreeRow(2L, 1L, "Spring"),
					new FolderTreeRow(3L, 1L, "DB"),
					new FolderTreeRow(4L, null, "개인"),
					new FolderTreeRow(5L, 4L, "일기")
			));

			// when
			var roots = folderService.getFolderTree(userId);

			// then
			assertThat(roots).hasSize(2);

			assertThat(roots).extracting(FolderTreeNodeResult::folderId)
					.containsExactlyInAnyOrder(1L, 4L);

			FolderTreeNodeResult study = roots.stream()
					.filter(r -> r.folderId().equals(1L))
					.findFirst()
					.orElseThrow();
			assertThat(study.children()).extracting(FolderTreeNodeResult::folderId)
					.containsExactly(3L, 2L);

			FolderTreeNodeResult personal = roots.stream()
					.filter(r -> r.folderId().equals(4L))
					.findFirst()
					.orElseThrow();
			assertThat(personal.children()).extracting(FolderTreeNodeResult::folderId)
					.containsExactly(5L);

			verify(folderRepository).findTreeRows(userId);
		}

		@Test
		@DisplayName("폴더가 없으면 빈 트리를 반환한다.")
		void getFolderTree_returnsEmptyWhenNoFolders() {
			// given
			Long userId = 1L;
			given(folderRepository.findTreeRows(userId)).willReturn(List.of());

			// when
			var roots = folderService.getFolderTree(userId);

			// then
			assertThat(roots).isEmpty();
			verify(folderRepository).findTreeRows(userId);
		}

		@Test
		@DisplayName("부모가 조회 대상에 없으면(데이터 불일치) 해당 노드를 루트로 취급한다.")
		void getFolderTree_treatsOrphanAsRootWhenParentMissing() {
			// given
			Long userId = 1L;
			// parentId=999는 rows에 없음
			given(folderRepository.findTreeRows(userId)).willReturn(List.of(
					new FolderTreeRow(1L, null, "A"),
					new FolderTreeRow(2L, 999L, "Orphan")
			));

			// when
			var roots = folderService.getFolderTree(userId);

			// then
			assertThat(roots).hasSize(2);
			assertThat(roots).extracting(FolderTreeNodeResult::folderId).containsExactly(1L, 2L);
			assertThat(roots.get(1).parentId()).isEqualTo(999L);
			assertThat(roots.get(1).children()).isEmpty();
			verify(folderRepository).findTreeRows(userId);
		}
	}
}