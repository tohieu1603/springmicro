package com.hieu.user_profile_service.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String phone;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private LocalDate dateOfBirth;
    private String gender;
}
