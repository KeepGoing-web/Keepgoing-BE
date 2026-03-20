package com.keepgoing.keepgoing.auth;

import com.keepgoing.keepgoing.auth.controller.dto.LoginRequest;
import com.keepgoing.keepgoing.auth.controller.dto.LoginResponse;
import com.keepgoing.keepgoing.auth.controller.dto.MyInfoResponse;
import com.keepgoing.keepgoing.auth.controller.dto.SignupRequest;
import com.keepgoing.keepgoing.auth.controller.dto.SignupResponse;
import com.keepgoing.keepgoing.auth.service.dto.LoginCommand;
import com.keepgoing.keepgoing.auth.service.dto.LoginResult;
import com.keepgoing.keepgoing.auth.service.dto.MyInfoResult;
import com.keepgoing.keepgoing.auth.service.dto.SignupCommand;
import com.keepgoing.keepgoing.auth.service.dto.SignupResult;
import com.keepgoing.keepgoing.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    // Web DTO -> Command
    public SignupCommand toCommand(SignupRequest dto) {
        return new SignupCommand(
                dto.email(),
                dto.password(),
                dto.name()
        );
    }

    public SignupResult toSignupResult(User user) {
        return new SignupResult(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }

    public SignupResponse toResponse(SignupResult dto) {
        return new SignupResponse(
                dto.id(),
                dto.email(),
                dto.name()
        );
    }

    public LoginCommand toCommand(LoginRequest dto) {
        return new LoginCommand(
                dto.email(),
                dto.password()
        );
    }

    public LoginResponse toResponse(LoginResult dto) {
        return new LoginResponse(
                dto.userId(),
                dto.email()
        );
    }

    public MyInfoResponse toResponse(MyInfoResult dto) {
        return new MyInfoResponse(
                dto.userId(),
                dto.email(),
                dto.name(),
                dto.role()
        );
    }
}
