package com.college.ledgermate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthDtos {

    @Data
    public static class RegisterRequest {
        // Explicit getters/setters so this DTO works even if Lombok
        // annotation processing is not configured in your build/IDE.
        @NotBlank
        private String name;

        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;

        // public void setName(String name) {
        // this.name = name;
        // }

        // public void setEmail(String email) {
        // this.email = email;
        // }

        // public void setPassword(String password) {
        // this.password = password;
        // }
    }

    @Data
    public static class UserResponse {

        private String name;
        private String email;

    }

    @Data
    public static class UpdateRequest {
        @NotBlank
        private String name;


        // Null or empty means keep existing password
        private String password;
    }
}
