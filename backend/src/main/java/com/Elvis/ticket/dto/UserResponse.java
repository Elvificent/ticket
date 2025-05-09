package com.Elvis.ticket.dto;

import com.Elvis.ticket.model.User;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String phoneNumber;
    private LocalDateTime createdAt;

    public static UserResponse fromUser(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
} 
 