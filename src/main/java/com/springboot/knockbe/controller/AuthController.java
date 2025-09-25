package com.springboot.knockbe.controller;

import com.springboot.knockbe.service.KakaoOAuth2Service;
import com.springboot.knockbe.service.TokenBlacklistService;
import com.springboot.knockbe.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "카카오 소셜 로그인 API")
@CrossOrigin(origins = {"http://localhost:5173", "https://www.knock-knock.site"})
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final KakaoOAuth2Service kakaoOAuth2Service;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(KakaoOAuth2Service kakaoOAuth2Service, JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.kakaoOAuth2Service = kakaoOAuth2Service;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @GetMapping("/kakao/login")
    @Operation(summary = "카카오 로그인 URL 생성", description = "카카오 로그인 페이지로 리다이렉트할 URL을 반환합니다.")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl() {
        String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code";

        Map<String, String> response = new HashMap<>();
        response.put("loginUrl", kakaoLoginUrl);
        response.put("message", "카카오 로그인 URL이 생성되었습니다.");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/kakao/callback")
    @Operation(summary = "카카오 로그인 콜백", description = "카카오에서 인증 후 호출되는 콜백 엔드포인트입니다.")
    public ResponseEntity<Void> kakaoCallback(
            @Parameter(description = "카카오에서 전달받은 인증 코드")
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            HttpServletResponse response) throws IOException {

        try {
            // 에러가 있는 경우 처리
            if (error != null) {
                log.error("카카오 로그인 에러 - error: {}, description: {}", error, error_description);

                // 에러 시 프론트엔드로 리다이렉트 (에러 파라미터 포함)
                response.sendRedirect("https://knock-knock.site/login?error=login_failed");
                return ResponseEntity.status(HttpStatus.FOUND).build();
            }

            // code가 없는 경우
            if (code == null || code.trim().isEmpty()) {
                log.warn("인증 코드가 전달되지 않음");

                response.sendRedirect("https://knock-knock.site/login?error=login_failed");
                return ResponseEntity.status(HttpStatus.FOUND).build();
            }

            log.info("카카오 로그인 콜백 시작 - code: {}", code.substring(0, Math.min(code.length(), 20)) + "...");

            // JWT 토큰인지 확인 (JWT는 '.'으로 구분된 3개 부분으로 구성)
            if (isJwtToken(code)) {
                log.warn("JWT 토큰이 code로 전달됨. 이미 로그인된 상태입니다.");

                // 이미 토큰이 있는 경우 바로 메인 페이지로
                response.sendRedirect("https://knock-knock.site/?token=" + code);
                return ResponseEntity.status(HttpStatus.FOUND).build();
            }

            // 카카오 인증 코드 유효성 검증
            if (!isValidKakaoCode(code)) {
                log.warn("유효하지 않은 카카오 인증 코드: 길이={}, 시작문자={}",
                        code.length(), code.substring(0, Math.min(code.length(), 5)));

                response.sendRedirect("https://knock-knock.site/login?error=login_failed");
                return ResponseEntity.status(HttpStatus.FOUND).build();
            }

            // 카카오 로그인 처리
            String jwtToken = kakaoOAuth2Service.loginOrRegister(code);

            log.info("카카오 로그인 성공");

            // 성공 시 JWT 토큰과 함께 프론트엔드 메인 페이지로 리다이렉트
            response.sendRedirect("https://knock-knock.site/?token=" + jwtToken);
            return ResponseEntity.status(HttpStatus.FOUND).build();

        } catch (Exception e) {
            log.error("카카오 로그인 실패", e);

            // 예외 발생 시 에러 페이지로 리다이렉트
            response.sendRedirect("https://knock-knock.site/login?error=login_failed");
            return ResponseEntity.status(HttpStatus.FOUND).build();
        }
    }

    /**
     * JWT 토큰인지 확인하는 메서드
     */
    private boolean isJwtToken(String token) {
        if (token == null) return false;

        // JWT는 점(.)으로 구분된 3개 부분으로 구성
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;

        // 각 부분이 Base64 형식인지 간단히 확인
        for (String part : parts) {
            if (part.isEmpty() || !part.matches("[A-Za-z0-9_-]+")) {
                return false;
            }
        }

        return true;
    }

    /**
     * 유효한 카카오 인증 코드인지 확인하는 메서드
     */
    private boolean isValidKakaoCode(String code) {
        if (code == null) return false;

        // 카카오 인증 코드는 일반적으로 20-100자 사이의 알파벳, 숫자, 특수문자 조합
        if (code.length() < 10 || code.length() > 200) {
            return false;
        }

        // JWT 토큰이 아니어야 함
        if (isJwtToken(code)) {
            return false;
        }

        return true;
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 토큰을 블랙리스트 처리하여 무효화합니다.")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Map<String, String> response = new HashMap<>();
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.put("message", "헤더에 Bearer 토큰이 없습니다. (이미 로그아웃 상태로 간주)");
            return ResponseEntity.ok(response);
        }
        String token = authorization.substring(7);
        if (!jwtUtil.validateToken(token)) {
            response.put("message", "유효하지 않은 토큰입니다. (이미 만료 또는 위조)");
            return ResponseEntity.ok(response);
        }
        long expMillis = jwtUtil.getExpirationDate(token).getTime();
        tokenBlacklistService.blacklist(token, expMillis);
        response.put("message", "로그아웃 완료 (토큰 무효화)");
        return ResponseEntity.ok(response);
    }
}
