package com.hieu.user_profile_service.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Value
@Builder
public class UserProfileDTO {
    String userId;
    String email;
    String phone;
    String firstName;
    String lastName;
    String avatarUrl;
    LocalDate dateOfBirth;
    String gender;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
