package com.leaderapp.leader_backend.dtos;

import lombok.Data;

@Data
public class CreateMonitorRequest {
    private String email;
    private String password;
    private String fullName;
}
