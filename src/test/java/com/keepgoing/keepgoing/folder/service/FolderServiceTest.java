package com.keepgoing.keepgoing.folder.service;

import static com.keepgoing.keepgoing.support.UserFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.folder.repository.dto.FolderTreeRow;
import com.keepgoing.keepgoing.folder.service.dto.FolderCreateCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderMoveCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderRenameCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderSummaryResult;
import com.keepgoing.keepgoing.folder.service.dto.FolderTreeNodeResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.note.repository.NoteRepository;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FolderServiceTest {

	public static final String DIRECTORY_NAME = "backend";
	@Mock
	UserRepository userRepository;

	@Mock
	FolderRepository folderRepository;

	@Mock
	NoteRepository noteRepository;

	@Mock
	FolderLockService folderLockService;

	@InjectMocks
	FolderService folderService;

	@Nested
	@DisplayName("createFolder()")
	class CreateFolder {

		@Test
		@DisplayName("부모가 없으면 루트 폴더를 생성한다.")
		void createFolder_createsRootFolder() {
			// given
			Long userId = 1L;
			User user = user(userId, "test@test.com", "test");
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
		@DisplayName("이름 앞뒤 공백을 제거한 뒤 중복 검사와 저장에 사용한다.")
		void createFolder_trimsNameBeforeDuplicateCheckAndSave() {
			// given
			Long userId = 1L;
			User user = user(userId, "test@test.com", "test");
			FolderCreateCommand command = new FolderCreateCommand(userId, null, "   backend   ");

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.existsRootFolder(userId, DIRECTORY_NAME)).willReturn(false);
			given(folderRepository.save(any(Folder.class))).willAnswer(invocation -> {
				Folder folder = invocation.getArgument(0);
				ReflectionTestUtils.setField(folder, "id", 10L);
				return folder;
			});

			// when
			FolderSummaryResult result = folderService.createFolder(command);

			// then
			ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);

			assertThat(result.folderId()).isEqualTo(10L);
			assertThat(result.parentId()).isNull();
			assertThat(result.name()).isEqualTo(DIRECTORY_NAME);

			verify(userRepository).findById(userId);
			verify(folderRepository).existsRootFolder(userId, DIRECTORY_NAME);
			verify(folderRepository).save(folderCaptor.capture());
			verifyNoMoreInteractions(userRepository, folderRepository);

			assertThat(folderCaptor.getValue().getName()).isEqualTo(DIRECTORY_NAME);
			assertThat(folderCaptor.getValue().getParent()).isNull();
		}

		@Test
		@DisplayName("부모가 있으면 하위 폴더를 생성한다.")
		void createFolder_createsChildFolder() {
			// given
			Long userId = 1L;
			Long parentId = 2L;
			User user = user(userId, "test@test.com", "test");
			Folder parent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			FolderCreateCommand command = new FolderCreateCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderLockService.lockActiveFolder(parentId)).willReturn(parent);
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
			verify(folderLockService).lockActiveFolder(parentId);
			verify(folderRepository).existsChildFolder(userId, parentId, DIRECTORY_NAME);
			verify(folderRepository).save(any(Folder.class));
			verifyNoMoreInteractions(userRepository, folderRepository, folderLockService);
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
			User user = user(userId);

			FolderCreateCommand command = new FolderCreateCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderLockService.lockActiveFolder(parentId))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);
			verify(userRepository).findById(userId);
			verify(folderLockService).lockActiveFolder(parentId);
			verifyNoMoreInteractions(userRepository, folderRepository, folderLockService);
		}

		@Test
		@DisplayName("다른 사용자의 부모 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다.")
		void createFolder_throwsWhenParentOwnedByAnotherUser() {
			// given
			Long userId = 1L;
			Long parentId = 2L;

			User user = user(userId);
			User otherUser = user(99L);

			Folder parent = Folder.create(otherUser, null, "other-root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			FolderCreateCommand command = new FolderCreateCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderLockService.lockActiveFolder(parentId)).willReturn(parent);

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);

			verify(userRepository).findById(userId);
			verify(folderLockService).lockActiveFolder(parentId);
			verifyNoMoreInteractions(userRepository, folderRepository, folderLockService);
		}

		@Test
		@DisplayName("이름이 유효하지 않으면 중복 조회 전에 FOLDER_INVALID_NAME 예외가 발생한다.")
		void createFolder_throwsWhenNameIsInvalidBeforeDuplicationCheck() {
			// given
			Long userId = 1L;
			User user = user(userId);
			FolderCreateCommand command = new FolderCreateCommand(userId, null, "   ");

			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_INVALID_NAME);

			verify(userRepository).findById(userId);
			verifyNoMoreInteractions(userRepository, folderRepository);
		}

		@Test
		@DisplayName("같은 루트 이름이 이미 있으면 FOLDER_NAME_DUPLICATED 예외가 발생한다.")
		void createFolder_throwsWhenRootFolderNameDuplicated() {
			// given
			Long userId = 1L;
			User user = user(userId);

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

			User user = user(userId);
			Folder parent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			FolderCreateCommand command = new FolderCreateCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderLockService.lockActiveFolder(parentId)).willReturn(parent);
			given(folderRepository.existsChildFolder(userId, parentId, DIRECTORY_NAME)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.createFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NAME_DUPLICATED);

			verify(userRepository).findById(userId);
			verify(folderLockService).lockActiveFolder(parentId);
			verify(folderRepository).existsChildFolder(userId, parentId, DIRECTORY_NAME);
			verifyNoMoreInteractions(userRepository, folderRepository, folderLockService);
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
			User user = user(userId);

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
			User user = user(userId);

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

			User other = user(99L);
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
		@DisplayName("전체 폴더를 1번 조회해서 targetParentId 기반 트리 구조로 조립한다.")
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
					.filter(r -> r.folderId()
							.equals(1L))
					.findFirst()
					.orElseThrow();
			assertThat(study.children()).extracting(FolderTreeNodeResult::folderId)
					.containsExactly(3L, 2L);

			FolderTreeNodeResult personal = roots.stream()
					.filter(r -> r.folderId()
							.equals(4L))
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
			// targetParentId=999는 rows에 없음
			given(folderRepository.findTreeRows(userId)).willReturn(List.of(
					new FolderTreeRow(1L, null, "A"),
					new FolderTreeRow(2L, 999L, "Orphan")
			));

			// when
			var roots = folderService.getFolderTree(userId);

			// then
			assertThat(roots).hasSize(2);
			assertThat(roots).extracting(FolderTreeNodeResult::folderId)
					.containsExactly(1L, 2L);
			assertThat(roots.get(1)
					.parentId()).isEqualTo(999L);
			assertThat(roots.get(1)
					.children()).isEmpty();
			verify(folderRepository).findTreeRows(userId);
		}

		@Test
		@DisplayName("트리 깊이가 비정상적으로 깊으면 MAX_TREE_DEPTH에서 잘라서 반환한다.")
		void getFolderTree_truncatesWhenDepthTooDeep() {
			// given
			Long userId = 1L;
			int maxDepth = (int) ReflectionTestUtils.getField(FolderService.class, "MAX_TREE_DEPTH");
			int chainLen = maxDepth + 5;

			List<FolderTreeRow> rows = new ArrayList<>(chainLen);
			rows.add(new FolderTreeRow(1L, null, "root"));
			for (int i = 2; i <= chainLen; i++) {
				rows.add(new FolderTreeRow((long) i, (long) (i - 1), "n" + i));
			}

			given(folderRepository.findTreeRows(userId)).willReturn(rows);

			// when
			List<FolderTreeNodeResult> roots = folderService.getFolderTree(userId);

			// then
			assertThat(roots).hasSize(1);

			FolderTreeNodeResult current = roots.get(0);

			for (int d = 0; d < maxDepth; d++) {
				assertThat(current.children())
						.as("depth=%s에서 child가 1개 존재해야 한다", d)
						.hasSize(1);
				current = current.children().get(0);
			}

			assertThat(current.children()).isEmpty();

			verify(folderRepository).findTreeRows(userId);
		}
	}

	@Nested
	@DisplayName("renameFolder()")
	class RenameFolder {

		@Test
		@DisplayName("루트 폴더 이름을 변경한다(trim 적용)")
		void renameFolder_renamesRootFolderWithTrim() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "old");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderRenameCommand command = new FolderRenameCommand(
					userId,
					folderId,
					"   test   "
			);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderRepository.existsRootFolder(userId, "test")).willReturn(false);

			// when
			FolderSummaryResult result = folderService.renameFolder(command);

			// then
			assertThat(result.folderId()).isEqualTo(folderId);
			assertThat(result.parentId()).isNull();
			assertThat(result.name()).isEqualTo("test");

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderRepository).existsRootFolder(userId, "test");
			verifyNoMoreInteractions(folderRepository);
		}

		@Test
		@DisplayName("폴더가 없으면 FOLDER_NOT_FOUND 예외가 발생한다")
		void renameFolder_throwsWhenFolderNotFound() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			FolderRenameCommand command = new FolderRenameCommand(userId, folderId, "test");

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> folderService.renameFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(folderRepository);
		}

		@Test
		@DisplayName("다른 사용자의 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다")
		void renameFolder_throwWhenFolderOwnedByAnotherUser() {
			// given
			Long userId = 1L;
			Long folderId = 10L;

			User other = user(99L);
			Folder folder = Folder.create(other, null, "old");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderRenameCommand command = new FolderRenameCommand(userId, folderId, "test");

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));

			// when & then
			assertThatThrownBy(() -> folderService.renameFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(folderRepository);
		}

		@Test
		@DisplayName("같은 루트 이름이 이미 있으면 FOLDER_NAME_DUPLICATED 예외가 발생한다")
		void renameFolder_throwsWhenRootNameDuplicated() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "old");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderRenameCommand command = new FolderRenameCommand(userId, folderId, "test");

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderRepository.existsRootFolder(userId, "test")).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.renameFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NAME_DUPLICATED);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderRepository).existsRootFolder(userId, "test");
			verifyNoMoreInteractions(folderRepository);
		}

		@Test
		@DisplayName("같은 부모 아래 같은 이름이 이미 있으면 FOLDER_NAME_DUPLICATED 예외가 발생한다")
		void renameFolder_throwsWhenChildNameDuplicated() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long parentId = 2L;
			User user = user(userId);

			Folder parent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(parent, "id", parentId);

			Folder folder = Folder.create(user, parent, "old");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderRenameCommand command = new FolderRenameCommand(userId, folderId, "test");

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderRepository.existsChildFolder(userId, parentId, "test")).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.renameFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NAME_DUPLICATED);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderRepository).existsChildFolder(userId, parentId, "test");
			verifyNoMoreInteractions(folderRepository);
		}
	}

	@Nested
	@DisplayName("deleteFolder()")
	class DeleteFolder {

		@Test
		@DisplayName("자식 폴더가 없으면 soft delete 처리한다.")
		void deleteFolder_softDeletesWhenNoChildren() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(folder, "id", folderId);

			given(folderLockService.lockActiveFolder(folderId)).willReturn(folder);
			given(folderRepository.existsByOwner_IdAndParent_IdAndDeletedAtIsNull(userId, folderId)).willReturn(false);

			// when
			folderService.deleteFolder(userId, folderId);

			// then
			Object deletedAt = ReflectionTestUtils.getField(folder, "deletedAt");
			assertThat(deletedAt).isNotNull();
			assertThat(folder.isDeleted()).isTrue();
			assertThat(folder.getDeletedAt()).isNotNull();

			verify(folderLockService).lockActiveFolder(folderId);
			verify(folderRepository).existsByOwner_IdAndParent_IdAndDeletedAtIsNull(userId, folderId);
			verify(noteRepository).existsByFolder_IdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(folderRepository, noteRepository, folderLockService);
		}

		@Test
		@DisplayName("폴더가 없으면 FOLDER_NOT_FOUND 예외가 발생한다")
		void deleteFolder_throwsWhenFolderNotFound() {
			// given
			Long userId = 1L;
			Long folderId = 10L;

			given(folderLockService.lockActiveFolder(folderId))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> folderService.deleteFolder(userId, folderId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);

			verify(folderLockService).lockActiveFolder(folderId);
			verifyNoMoreInteractions(folderRepository, noteRepository, folderLockService);
		}

		@Test
		@DisplayName("다른 사용자의 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다")
		void deleteFolder_throwsWhenFolderOwnedByAnotherUser() {
			// given
			Long userId = 1L;
			Long folderId = 10L;

			User other = user(99L);
			Folder folder = Folder.create(other, null, "root");
			ReflectionTestUtils.setField(folder, "id", folderId);

			given(folderLockService.lockActiveFolder(folderId)).willReturn(folder);

			// when & then
			assertThatThrownBy(() -> folderService.deleteFolder(userId, folderId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);
			verify(folderLockService).lockActiveFolder(folderId);
			verifyNoMoreInteractions(folderRepository, noteRepository, folderLockService);
		}

		@Test
		@DisplayName("자식 폴더가 있으면 FOLDER_NOT_EMPTY 예외가 발생한다")
		void deleteFolder_throwsWhenFolderHasChildren() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(folder, "id", folderId);

			given(folderLockService.lockActiveFolder(folderId)).willReturn(folder);
			given(folderRepository.existsByOwner_IdAndParent_IdAndDeletedAtIsNull(userId, folderId)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.deleteFolder(userId, folderId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_EMPTY);
			verify(folderLockService).lockActiveFolder(folderId);
			verify(folderRepository).existsByOwner_IdAndParent_IdAndDeletedAtIsNull(userId, folderId);
			verifyNoMoreInteractions(folderRepository, noteRepository, folderLockService);
		}

		@Test
		@DisplayName("노트가 있으면 FOLDER_NOT_EMPTY 예외가 발생한다")
		void deleteFolder_throwsWhenFolderHasNotes() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(folder, "id", folderId);

			given(folderLockService.lockActiveFolder(folderId)).willReturn(folder);
			given(folderRepository.existsByOwner_IdAndParent_IdAndDeletedAtIsNull(userId, folderId)).willReturn(false);
			given(noteRepository.existsByFolder_IdAndDeletedAtIsNull(folderId)).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.deleteFolder(userId, folderId))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_EMPTY);
			verify(folderLockService).lockActiveFolder(folderId);
			verify(folderRepository).existsByOwner_IdAndParent_IdAndDeletedAtIsNull(userId, folderId);
			verify(noteRepository).existsByFolder_IdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(folderRepository, noteRepository, folderLockService);
		}
	}

	@Nested
	@DisplayName("moveFolder()")
	class MoveFolder {

		@Test
		@DisplayName("루트 폴더를 다른 부모 아래로 이동한다")
		void moveFolder_movesRootFolderToAnotherParent() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long targetParentId = 20L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			Folder targetParent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(targetParent, "id", targetParentId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, targetParentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(null, targetParentId)))
					.willReturn(Map.of(targetParentId, targetParent));
			given(folderRepository.findTreeRows(userId)).willReturn(List.of(
					new FolderTreeRow(folderId, null, "backend"),
					new FolderTreeRow(targetParentId, null, "root")
			));
			given(folderRepository.existsChildFolder(userId, targetParentId, "backend")).willReturn(false);

			// when
			FolderSummaryResult result = folderService.moveFolder(command);

			// then
			assertThat(result.folderId()).isEqualTo(folderId);
			assertThat(result.parentId()).isEqualTo(targetParentId);
			assertThat(result.name()).isEqualTo("backend");
			assertThat(folder.getParent()).isEqualTo(targetParent);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(null, targetParentId));
			verify(folderRepository).findTreeRows(userId);
			verify(folderRepository).existsChildFolder(userId, targetParentId, "backend");
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("하위 폴더를 다른 부모 아래로 이동할 때 현재 부모와 대상 부모를 함께 잠근다")
		void moveFolder_locksCurrentParentAndTargetParentTogether() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long currentParentId = 20L;
			Long targetParentId = 30L;
			User user = user(userId);

			Folder currentParent = Folder.create(user, null, "current");
			ReflectionTestUtils.setField(currentParent, "id", currentParentId);

			Folder targetParent = Folder.create(user, null, "target");
			ReflectionTestUtils.setField(targetParent, "id", targetParentId);

			Folder folder = Folder.create(user, currentParent, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, targetParentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(currentParentId, targetParentId)))
					.willReturn(Map.of(currentParentId, currentParent, targetParentId, targetParent));
			given(folderRepository.findTreeRows(userId)).willReturn(List.of(
					new FolderTreeRow(folderId, currentParentId, "backend"),
					new FolderTreeRow(currentParentId, null, "current"),
					new FolderTreeRow(targetParentId, null, "target")
			));
			given(folderRepository.existsChildFolder(userId, targetParentId, "backend")).willReturn(false);

			// when
			FolderSummaryResult result = folderService.moveFolder(command);

			// then
			assertThat(result.parentId()).isEqualTo(targetParentId);
			assertThat(folder.getParent()).isEqualTo(targetParent);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(currentParentId, targetParentId));
			verify(folderRepository).findTreeRows(userId);
			verify(folderRepository).existsChildFolder(userId, targetParentId, "backend");
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("하위 폴더를 루트로 이동한다")
		void moveFolder_movesChildFolderToRoot() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long currentParentId = 20L;
			User user = user(userId);

			Folder currentParent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(currentParent, "id", currentParentId);

			Folder folder = Folder.create(user, currentParent, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, null);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(currentParentId, null)))
					.willReturn(Map.of(currentParentId, currentParent));
			given(folderRepository.existsRootFolder(userId, "backend")).willReturn(false);

			// when
			FolderSummaryResult result = folderService.moveFolder(command);

			// then
			assertThat(result.folderId()).isEqualTo(folderId);
			assertThat(result.parentId()).isNull();
			assertThat(result.name()).isEqualTo("backend");
			assertThat(folder.getParent()).isNull();

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(currentParentId, null));
			verify(folderRepository).existsRootFolder(userId, "backend");
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("같은 부모로 이동하면 그대로 반환한다")
		void moveFolder_returnsCurrentFolderWhenTargetParentIsSame() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long currentParentId = 20L;
			User user = user(userId);

			Folder currentParent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(currentParent, "id", currentParentId);

			Folder folder = Folder.create(user, currentParent, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, currentParentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));

			// when
			FolderSummaryResult result = folderService.moveFolder(command);

			// then
			assertThat(result.folderId()).isEqualTo(folderId);
			assertThat(result.parentId()).isEqualTo(currentParentId);
			assertThat(result.name()).isEqualTo("backend");
			assertThat(folder.getParent()).isEqualTo(currentParent);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(folderRepository);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("폴더가 없으면 FOLDER_NOT_FOUND 예외가 발생한다")
		void moveFolder_throwsWhenFolderNotFound() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, 20L);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(folderRepository);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("다른 사용자의 폴더면 FOLDER_ACCESS_DENIED 예외가 발생한다")
		void moveFolder_throwsWhenFolderOwnedByAnotherUser() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			User other = user(99L);
			Folder folder = Folder.create(other, null, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, 20L);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(folderRepository);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("새 부모 폴더가 없으면 FOLDER_NOT_FOUND 예외가 발생한다")
		void moveFolder_throwsWhenTargetParentNotFound() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long targetParentId = 20L;
			User user = user(userId);
			Folder folder = Folder.create(user, null, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, targetParentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(null, targetParentId)))
					.willThrow(new BusinessException(ErrorCode.FOLDER_NOT_FOUND));

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NOT_FOUND);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(null, targetParentId));
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("다른 사용자의 부모 폴더로는 이동할 수 없다")
		void moveFolder_throwsWhenTargetParentOwnedByAnotherUser() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long targetParentId = 20L;
			User user = user(userId);
			User other = user(99L);

			Folder folder = Folder.create(user, null, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			Folder targetParent = Folder.create(other, null, "other-root");
			ReflectionTestUtils.setField(targetParent, "id", targetParentId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, targetParentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(null, targetParentId)))
					.willReturn(Map.of(targetParentId, targetParent));

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_ACCESS_DENIED);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(null, targetParentId));
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("자기 자신 아래로는 이동할 수 없다")
		void moveFolder_throwsWhenTargetParentIsSelf() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			User user = user(userId);
			Folder folder = Folder.create(user, null, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, folderId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_MOVE_INVALID);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verifyNoMoreInteractions(folderRepository);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("하위 폴더 아래로는 이동할 수 없다")
		void moveFolder_throwsWhenTargetParentIsDescendant() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long targetParentId = 20L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			Folder targetParent = Folder.create(user, folder, "child");
			ReflectionTestUtils.setField(targetParent, "id", targetParentId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, targetParentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(null, targetParentId)))
					.willReturn(Map.of(targetParentId, targetParent));
			given(folderRepository.findTreeRows(userId)).willReturn(List.of(
					new FolderTreeRow(folderId, null, "backend"),
					new FolderTreeRow(targetParentId, folderId, "child")
			));

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_MOVE_INVALID);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(null, targetParentId));
			verify(folderRepository).findTreeRows(userId);
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("루트로 이동할 때 같은 이름이 있으면 FOLDER_NAME_DUPLICATED 예외가 발생한다")
		void moveFolder_throwsWhenRootNameDuplicated() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long currentParentId = 20L;
			User user = user(userId);

			Folder currentParent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(currentParent, "id", currentParentId);

			Folder folder = Folder.create(user, currentParent, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, null);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(currentParentId, null)))
					.willReturn(Map.of(currentParentId, currentParent));
			given(folderRepository.existsRootFolder(userId, "backend")).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NAME_DUPLICATED);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(currentParentId, null));
			verify(folderRepository).existsRootFolder(userId, "backend");
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("대상 부모 아래 같은 이름이 있으면 FOLDER_NAME_DUPLICATED 예외가 발생한다")
		void moveFolder_throwsWhenChildNameDuplicated() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long targetParentId = 20L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "backend");
			ReflectionTestUtils.setField(folder, "id", folderId);

			Folder targetParent = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(targetParent, "id", targetParentId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, targetParentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(null, targetParentId)))
					.willReturn(Map.of(targetParentId, targetParent));
			given(folderRepository.findTreeRows(userId)).willReturn(List.of(
					new FolderTreeRow(folderId, null, "backend"),
					new FolderTreeRow(targetParentId, null, "root")
			));
			given(folderRepository.existsChildFolder(userId, targetParentId, "backend")).willReturn(true);

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_NAME_DUPLICATED);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(null, targetParentId));
			verify(folderRepository).findTreeRows(userId);
			verify(folderRepository).existsChildFolder(userId, targetParentId, "backend");
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}

		@Test
		@DisplayName("부모 체인에 순환이 있으면 FOLDER_MOVE_INVALID 예외가 발생한다")
		void moveFolder_throwsWhenTargetParentLineageContaionsCycle() {
			// given
			Long userId = 1L;
			Long folderId = 10L;
			Long targetParentId = 20L;
			User user = user(userId);

			Folder folder = Folder.create(user, null, "root");
			ReflectionTestUtils.setField(folder, "id", folderId);

			Folder targetParent = Folder.create(user, null, "target");
			ReflectionTestUtils.setField(folder, "id", folderId);

			FolderMoveCommand command = new FolderMoveCommand(userId, folderId, targetParentId);

			given(folderRepository.findByIdAndDeletedAtIsNull(folderId)).willReturn(Optional.of(folder));
			given(folderLockService.lockActiveFolders(Arrays.asList(null, targetParentId)))
					.willReturn(Map.of(targetParentId, targetParent));
			given(folderRepository.findTreeRows(userId)).willReturn(List.of(
					new FolderTreeRow(folderId, null, "root"),
					new FolderTreeRow(20L, 30L, "target"),
					new FolderTreeRow(30L, 20L, "cycle")
			));

			// when & then
			assertThatThrownBy(() -> folderService.moveFolder(command))
					.isInstanceOf(BusinessException.class)
					.hasFieldOrPropertyWithValue("errorCode", ErrorCode.FOLDER_MOVE_INVALID);

			verify(folderRepository).findByIdAndDeletedAtIsNull(folderId);
			verify(folderLockService).lockActiveFolders(Arrays.asList(null, targetParentId));
			verify(folderRepository).findTreeRows(userId);
			verifyNoMoreInteractions(folderRepository, folderLockService);
			verifyNoInteractions(noteRepository, userRepository);
		}
	}
}
