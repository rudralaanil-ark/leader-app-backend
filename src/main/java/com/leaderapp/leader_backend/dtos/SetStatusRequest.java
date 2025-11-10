package com.leaderapp.leader_backend.dtos;

import lombok.Data;

@Data
public class SetStatusRequest {
    private String uid;
    private boolean status;
}
