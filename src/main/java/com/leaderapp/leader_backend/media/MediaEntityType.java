package com.leaderapp.leader_backend.media;

import lombok.Getter;

@Getter
public enum MediaEntityType {

    NEWS("news"),
    EVENTS("events"),
    PROFILE("profiles"),
    GALLERY("gallery"),
    COMPLAINT("complaints");

    private final String folder;

    MediaEntityType(String folder) {
        this.folder = folder;
    }

}
