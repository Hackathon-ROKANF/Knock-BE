package com.springboot.knockbe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KakaoUserInfo {
    private Long id;

    @JsonProperty("connected_at")
    private String connectedAt;

    private Properties properties;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    // Getter 메서드들
    public Long getId() {
        return id;
    }

    public String getConnectedAt() {
        return connectedAt;
    }

    public Properties getProperties() {
        return properties;
    }

    public KakaoAccount getKakaoAccount() {
        return kakaoAccount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Properties {
        private String nickname;

        @JsonProperty("profile_image")
        private String profileImage;

        @JsonProperty("thumbnail_image")
        private String thumbnailImage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoAccount {
        @JsonProperty("profile_nickname_needs_agreement")
        private Boolean profileNicknameNeedsAgreement;

        @JsonProperty("profile_image_needs_agreement")
        private Boolean profileImageNeedsAgreement;

        private Profile profile;

        @JsonProperty("has_email")
        private Boolean hasEmail;

        @JsonProperty("email_needs_agreement")
        private Boolean emailNeedsAgreement;

        @JsonProperty("is_email_valid")
        private Boolean isEmailValid;

        @JsonProperty("is_email_verified")
        private Boolean isEmailVerified;

        private String email;

        // Getter 메서드들
        public Profile getProfile() {
            return profile;
        }

        public String getEmail() {
            return email;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Profile {
            private String nickname;

            @JsonProperty("thumbnail_image_url")
            private String thumbnailImageUrl;

            @JsonProperty("profile_image_url")
            private String profileImageUrl;

            @JsonProperty("is_default_image")
            private Boolean isDefaultImage;

            // Getter 메서드들
            public String getNickname() {
                return nickname;
            }

            public String getProfileImageUrl() {
                return profileImageUrl;
            }

            public String getThumbnailImageUrl() {
                return thumbnailImageUrl;
            }
        }
    }
}
