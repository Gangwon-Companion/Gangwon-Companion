package com.gangwon.companion.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String username;

    @Column(nullable = false)
    private String password;

    @jakarta.persistence.Convert(converter = com.gangwon.companion.global.security.AesGcmAttributeConverter.class)
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_hash", unique = true, length = 64)
    private String emailHash;

    @Column(nullable = false, unique = true, length = 6)
    private String nickname;

    @Column(name = "profile_image_s3_key", length = 500)
    private String profileImageS3Key;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean withdrawn = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String username, String password, String email, String emailHash, String nickname) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.emailHash = emailHash;
        this.nickname = nickname;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeProfileImage(String profileImageS3Key) {
        this.profileImageS3Key = profileImageS3Key;
    }

    public void withdraw() {
        this.withdrawn = true;
    }
}
