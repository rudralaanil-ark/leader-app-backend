package com.leaderapp.leader_backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateNewsRequest {
    private String title;
    private String description;
    private String imageUrl;
    private String videoUrl;
    private String createdBy;
}
