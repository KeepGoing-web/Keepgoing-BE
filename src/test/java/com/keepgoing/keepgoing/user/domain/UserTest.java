package com.keepgoing.keepgoing.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	@DisplayName("create: 기본값과 필수 필드를 설정한다")
	void create_setsRequiredFieldsAndDefaults() {
		// given
		String email = "test@example.com";
		String name = "테스트유저";

		// when
		User user = User.create(email, name);

		// then
		assertThat(user.getId()).isNull();
		assertThat(user.getEmail()).isEqualTo(email);
		assertThat(user.getName()).isEqualTo(name);
		assertThat(user.getRole()).isEqualTo(UserRole.USER);
		assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(user.getLastLoginAt()).isNull();
	}

	@Nested
	@DisplayName("changeName()")
	class ChangeName {

		@Test
		@DisplayName("유효한 이름이면 사용자 이름을 변경한다")
		void changeName_updatesName() {
			User user = User.create("test@example.com", "기존 이름");

			user.changeName("새 이름");

			assertThat(user.getName()).isEqualTo("새 이름");
		}

		@Test
		@DisplayName("이름이 null이면 INVALID_INPUT을 던진다")
		void changeName_throwsWhenNameIsNull() {
			User user = User.create("test@example.com", "기존 이름");

			assertThatThrownBy(() -> user.changeName(null))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT);
		}

		@Test
		@DisplayName("이름이 공백이면 INVALID_INPUT을 던진다")
		void changeName_throwsWhenNameIsBlank() {
			User user = User.create("test@example.com", "기존 이름");

			assertThatThrownBy(() -> user.changeName("   "))
					.isInstanceOf(BusinessException.class)
					.extracting("errorCode")
					.isEqualTo(ErrorCode.INVALID_INPUT);
		}
	}
}
