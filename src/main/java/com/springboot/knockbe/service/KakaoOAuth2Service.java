package com.springboot.knockbe.service;

import com.springboot.knockbe.dto.KakaoTokenResponse;
import com.springboot.knockbe.dto.KakaoUserInfo;
import com.springboot.knockbe.entity.User;
import com.springboot.knockbe.repository.UserRepository;
import com.springboot.knockbe.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
public class KakaoOAuth2Service {

    private static final Logger log = LoggerFactory.getLogger(KakaoOAuth2Service.class);
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final WebClient webClient;

    public KakaoOAuth2Service(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    public String getKakaoAccessToken(String code) {
        try {
            log.info("카카오 액세스 토큰 요청 시작");

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientId);
            params.add("redirect_uri", redirectUri);
            params.add("code", code);

            KakaoTokenResponse response = webClient.post()
                    .uri(tokenUri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(BodyInserters.fromFormData(params))
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || response.getAccessToken() == null) {
                log.error("카카오 토큰 응답이 null이거나 access_token이 없음");
                throw new RuntimeException("Failed to get access token from Kakao - null response");
            }

            log.info("카카오 액세스 토큰 획득 성공");
            return response.getAccessToken();

        } catch (WebClientResponseException e) {
            log.error("카카오 토큰 요청 실패 - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get access token from Kakao: " + e.getMessage());
        } catch (Exception e) {
            log.error("카카오 토큰 요청 중 예외 발생", e);
            throw new RuntimeException("Failed to get access token from Kakao: " + e.getMessage());
        }
    }

    public KakaoUserInfo getKakaoUserInfo(String accessToken) {
        try {
            log.info("카카오 사용자 정보 요청 시작");

            KakaoUserInfo userInfo = webClient.get()
                    .uri(userInfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserInfo.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (userInfo == null) {
                log.error("카카오 사용자 정보 응답이 null");
                throw new RuntimeException("Failed to get user info from Kakao - null response");
            }

            log.info("카카오 사용자 정보 획득 성공 - ID: {}", userInfo.getId());
            return userInfo;

        } catch (WebClientResponseException e) {
            log.error("카카오 사용자 정보 요청 실패 - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to get user info from Kakao: " + e.getMessage());
        } catch (Exception e) {
            log.error("카카오 사용자 정보 요청 중 예외 발생", e);
            throw new RuntimeException("Failed to get user info from Kakao: " + e.getMessage());
        }
    }

    public String loginOrRegister(String code) {
        try {
            log.info("카카오 로그인/등록 프로세스 시작");

            // 1. 카카오로부터 액세스 토큰 획득
            String accessToken = getKakaoAccessToken(code);

            // 2. 액세스 토큰으로 사용자 정보 조회
            KakaoUserInfo kakaoUserInfo = getKakaoUserInfo(accessToken);

            // 3. 사용자 정보 추출 및 검증
            Long kakaoId = kakaoUserInfo.getId();
            if (kakaoId == null) {
                throw new RuntimeException("카카오 사용자 ID가 없습니다.");
            }

            String nickname = null;
            String profileImage = null;

            // 카카오 계정 정보 안전하게 추출
            if (kakaoUserInfo.getKakaoAccount() != null && kakaoUserInfo.getKakaoAccount().getProfile() != null) {
                nickname = kakaoUserInfo.getKakaoAccount().getProfile().getNickname();
                profileImage = kakaoUserInfo.getKakaoAccount().getProfile().getProfileImageUrl();
            }

            // 닉네임이 없는 경우 기본값 설정
            if (nickname == null || nickname.trim().isEmpty()) {
                nickname = "카카오사용자" + kakaoId;
            }

            log.info("카카오 사용자 정보 - ID: {}, Nickname: {}", kakaoId, nickname);

            // 4. 기존 사용자 확인 또는 새 사용자 생성
            // final 변수로 선언하여 람다에서 사용 가능하게 함
            final Long finalKakaoId = kakaoId;
            final String finalNickname = nickname;
            final String finalProfileImage = profileImage;

            User user = userRepository.findByKakaoId(finalKakaoId)
                    .orElseGet(() -> {
                        log.info("새 사용자 생성 - kakaoId: {}", finalKakaoId);
                        User newUser = new User(finalKakaoId, finalNickname, finalProfileImage);
                        return userRepository.save(newUser);
                    });

            log.info("사용자 처리 완료 - userId: {}", user.getId());

            // 5. JWT 토큰 생성 및 반환
            String jwtToken = jwtUtil.generateToken(user.getId());

            log.info("JWT 토큰 생성 완료");
            return jwtToken;

        } catch (Exception e) {
            log.error("카카오 로그인/등록 프로세스 실패", e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }
}
