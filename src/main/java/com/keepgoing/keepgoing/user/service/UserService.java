package com.keepgoing.keepgoing.user.service;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.keepgoing.keepgoing.user.domain.User;
import com.keepgoing.keepgoing.user.repository.UserRepository;
import com.keepgoing.keepgoing.user.service.dto.UserInfoResult;
import com.keepgoing.keepgoing.user.service.dto.UserUpdateCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public UserInfoResult getMyInfo(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		return UserInfoResult.from(user);
	}

	@Transactional
	public UserInfoResult updateMyProfile(UserUpdateCommand command) {
		User user = userRepository.findById(command.userId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		String newName = normalizeName(command.name());
		user.changeName(newName);
		return UserInfoResult.from(user);
	}

	private String normalizeName(String name) {
		if (name == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		return name.trim();
	}
}
