package com.example.userservice.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String username;
    private String email;
    private String firstName;
    private String lastName;

    public LoginResponse() {
    }

    public LoginResponse(String token, String username, String email, String firstName, String lastName) {
        this.token = token;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
