package com.keepgoing.keepgoing.user.service;

import static com.keepgoing.keepgoing.support.UserFixture.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.support.UserFixture;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import com.keepgoing.keepgoing.user.service.dto.UserInfoResult;
import com.keepgoing.keepgoing.user.service.dto.UserUpdateCommand;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	UserRepository userRepository;

	@InjectMocks
	UserService userService;

	@Test
	@DisplayName("내 정보 조회 시 사용자 기본 정보를 반환한다")
	void getMyInfo_returnsUserInfo() {
		Long userId = 1L;
		User user = UserFixture.user(userId, "test@test.com", "홍길동");

		given(userRepository.findById(userId)).willReturn(java.util.Optional.of(user));

		UserInfoResult result = userService.getMyInfo(userId);

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.email()).isEqualTo("test@test.com");
		assertThat(result.name()).isEqualTo("홍길동");
		assertThat(result.role()).isEqualTo(UserRole.USER);
	}

	@Test
	@DisplayName("내 정보 조회 시 사용자가 없으면 USER_NOT_FOUND를 던진다")
	void getMyInfo_throwsWhenUserMissing() {
		Long userId = 1L;
		given(userRepository.findById(userId)).willReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> userService.getMyInfo(userId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.USER_NOT_FOUND);
	}

	@Nested
	@DisplayName("updateMyProfile()")
	class UpdateMyProfile {

		@Test
		@DisplayName("이름 앞뒤 공백을 제거하고 사용자 이름을 수정한다")
		void updateMyProfile_trimsAndUpdatesName() {
			Long userId = 1L;
			User user = user(userId, "test@test.com", "기존 이름");
			UserUpdateCommand command = new UserUpdateCommand(userId, "  새 이름  ");

			given(userRepository.findById(userId)).willReturn(Optional.of(user));

			UserInfoResult result = userService.updateMyProfile(command);

			assertThat(user.getName()).isEqualTo("새 이름");
			assertThat(result.userId()).isEqualTo(userId);
			assertThat(result.email()).isEqualTo("test@test.com");
			assertThat(result.name()).isEqualTo("새 이름");
			assertThat(result.role()).isEqualTo(UserRole.USER);
			verify(userRepository).findById(userId);
			verifyNoMoreInteractions(userRepository);
		}

		@Test
		@DisplayName("사용자가 없으면 USER_NOT_FOUND를 던진다")
		void updateMyProfile_throwsWhenUserMissing() {
			Long userId = 1L;
			UserUpdateCommand command = new UserUpdateCommand(userId, "새 이름");
			given(userRepository.findById(userId)).willReturn(Optional.empty());

			assertThatThrownBy(() -> userService.updateMyProfile(command))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.USER_NOT_FOUND);

			verify(userRepository).findById(userId);
			verifyNoMoreInteractions(userRepository);
		}

		@Test
		@DisplayName("이름이 null이면 INVALID_INPUT을 던진다")
		void updateMyProfile_throwsWhenNameIsNull() {
			Long userId = 1L;
			User user = user(userId, "test@test.com", "기존 이름");
			UserUpdateCommand command = new UserUpdateCommand(userId, null);
			given(userRepository.findById(userId)).willReturn(java.util.Optional.of(user));

			assertThatThrownBy(() -> userService.updateMyProfile(command))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT);

			assertThat(user.getName()).isEqualTo("기존 이름");
			verify(userRepository).findById(userId);
			verifyNoMoreInteractions(userRepository);
		}

		@Test
		@DisplayName("trim 이후 이름이 비면 INVALID_INPUT을 던진다")
		void updateMyProfile_throwsWhenNameIsBlankAfterTrim() {
			Long userId = 1L;
			User user = user(userId, "test@test.com", "기존 이름");
			UserUpdateCommand command = new UserUpdateCommand(userId, "   ");
			given(userRepository.findById(userId)).willReturn(java.util.Optional.of(user));

			assertThatThrownBy(() -> userService.updateMyProfile(command))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT);

			assertThat(user.getName()).isEqualTo("기존 이름");
			verify(userRepository).findById(userId);
			verifyNoMoreInteractions(userRepository);
		}
	}
}
