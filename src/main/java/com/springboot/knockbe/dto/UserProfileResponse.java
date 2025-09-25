package com.springboot.knockbe.dto;

public class UserProfileResponse {
    private String nickname;
    private String profileImage;

    public UserProfileResponse(String nickname, String profileImage) {
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImage() {
        return profileImage;
    }
}

