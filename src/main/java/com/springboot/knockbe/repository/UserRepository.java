package com.springboot.knockbe.repository;

import com.springboot.knockbe.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(Long kakaoId);
    boolean existsByKakaoId(Long kakaoId);
}
