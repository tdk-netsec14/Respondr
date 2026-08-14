package com.respondr.auth.dto;

import com.respondr.auth.User;
import com.respondr.auth.UserStatus;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String name,
        UserStatus status
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getStatus()
        );
    }
}
