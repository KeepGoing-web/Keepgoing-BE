package com.keepgoing.keepgoing.user.domain;

public enum UserRole {
    USER,
    ADMIN;

    public String toAuthority() {
        return "ROLE_" + this.name();
    }
}
