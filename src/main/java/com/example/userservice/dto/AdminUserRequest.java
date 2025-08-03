package com.example.userservice.dto;

import com.example.userservice.entity.Role;
import lombok.Data;

@Data
public class AdminUserRequest {
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private Boolean isActive;

    public AdminUserRequest() {}
}