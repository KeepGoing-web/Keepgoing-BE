package com.keepgoing.keepgoing.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
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
}
