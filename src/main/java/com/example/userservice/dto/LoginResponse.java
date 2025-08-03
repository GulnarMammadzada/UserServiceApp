package com.example.userservice.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;

    public LoginResponse() {}

    public LoginResponse(String accessToken, String refreshToken, String username, String email, String firstName, String lastName, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }
}