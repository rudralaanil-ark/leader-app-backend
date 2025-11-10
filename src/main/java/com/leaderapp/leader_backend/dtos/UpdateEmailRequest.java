package com.leaderapp.leader_backend.dtos;

import lombok.Data;

@Data
public class UpdateEmailRequest {
    private String uid;
    private String newEmail;
}
