package com.keepgoing.keepgoing.auth;

import com.keepgoing.keepgoing.auth.controller.SignupRequest;
import com.keepgoing.keepgoing.auth.controller.SignupResponse;
import com.keepgoing.keepgoing.auth.service.SignupCommand;
import com.keepgoing.keepgoing.auth.service.SignupResult;
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

    public SignupResponse toResponse(SignupResult result) {
        return new SignupResponse(
                result.id(),
                result.email(),
                result.name()
        );
    }
}
