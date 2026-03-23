package com.keepgoing.keepgoing.folder.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.keepgoing.keepgoing.folder.domain.Folder;
import com.keepgoing.keepgoing.folder.repository.FolderRepository;
import com.keepgoing.keepgoing.folder.service.dto.CreateFolderCommand;
import com.keepgoing.keepgoing.folder.service.dto.FolderResult;
import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
			CreateFolderCommand command = new CreateFolderCommand(userId, null, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.existsRootFolder(userId, DIRECTORY_NAME)).willReturn(false);

			Folder saved = Folder.create(user, null, DIRECTORY_NAME);
			ReflectionTestUtils.setField(saved, "id", 10L);
			given(folderRepository.save(any(Folder.class))).willReturn(saved);

			// when
			FolderResult result = folderService.createFolder(command);

			// then
			assertThat(result.folderId()).isEqualTo(10L);
			assertThat(result.parentFolderId()).isNull();
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

			CreateFolderCommand command = new CreateFolderCommand(userId, parentId, DIRECTORY_NAME);

			given(userRepository.findById(userId)).willReturn(Optional.of(user));
			given(folderRepository.findByIdAndDeletedAtIsNull(parentId)).willReturn(Optional.of(parent));
			given(folderRepository.existsChildFolder(userId, parentId, DIRECTORY_NAME)).willReturn(false);

			Folder saved = Folder.create(user, parent, DIRECTORY_NAME);
			ReflectionTestUtils.setField(saved, "id", 11L);
			given(folderRepository.save(any(Folder.class))).willReturn(saved);

			// when
			FolderResult result = folderService.createFolder(command);

			// then
			assertThat(result.folderId()).isEqualTo(11L);
			assertThat(result.parentFolderId()).isEqualTo(parentId);
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
			CreateFolderCommand command = new CreateFolderCommand(userId, null, DIRECTORY_NAME);

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

			CreateFolderCommand command = new CreateFolderCommand(userId, parentId, DIRECTORY_NAME);

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

			CreateFolderCommand command = new CreateFolderCommand(userId, parentId, DIRECTORY_NAME);

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

			CreateFolderCommand command = new CreateFolderCommand(userId, null, DIRECTORY_NAME);

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

			CreateFolderCommand command = new CreateFolderCommand(userId, parentId, DIRECTORY_NAME);

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
}