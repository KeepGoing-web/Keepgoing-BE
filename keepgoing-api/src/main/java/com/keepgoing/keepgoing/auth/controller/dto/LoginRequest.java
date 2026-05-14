package com.keepgoing.keepgoing.auth.controller.dto;

import com.keepgoing.keepgoing.auth.validation.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Email
        @NotBlank
        String email,

        @Password
        String password
) {
}
