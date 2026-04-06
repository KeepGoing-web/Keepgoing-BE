package com.keepgoing.keepgoing.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.support.UserFixture;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.domain.UserRole;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import com.keepgoing.keepgoing.user.service.dto.UserInfoResult;
import org.junit.jupiter.api.DisplayName;
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
}
