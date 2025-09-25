package com.springboot.knockbe.controller;

import com.springboot.knockbe.dto.UserProfileResponse;
import com.springboot.knockbe.entity.User;
import com.springboot.knockbe.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@Tag(name = "사용자", description = "사용자 프로필 API")
@CrossOrigin(origins = {"http://localhost:5173", "https://www.knock-knock.site"})
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    @Operation(summary = "내 프로필 조회", description = "JWT 인증 필요. 닉네임과 프로필 이미지를 반환.")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid principal");
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        User user = userOpt.get();
        return ResponseEntity.ok(new UserProfileResponse(user.getNickname(), user.getProfileImage()));
    }
}

