package com.college.ledgermate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GroupDtos {

    @Data
    public static class CreateGroupRequest {
        @NotBlank
        private String name;

        @NotEmpty
        private List<String> memberEmails;
    }

    @Data
    public static class AddMemberRequest {
        @NotBlank
        private String email;
    }
}

