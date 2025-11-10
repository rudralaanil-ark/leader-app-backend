package com.leaderapp.leader_backend.dtos;

import lombok.Data;

@Data
public class UpdatePasswordRequest {
    private String uid;
    private String newPassword;
}
