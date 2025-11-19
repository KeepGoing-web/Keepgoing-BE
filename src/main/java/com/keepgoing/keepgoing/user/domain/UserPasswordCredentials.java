package com.keepgoing.keepgoing.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ID / 비밀번호 로그인 정보 엔티티
 * - User 엔티티에 1:1로 붙는 부속 엔티티
 * - PK = FK (user_id) 구조
 * - OAuth-only 계정은 이 엔티티가 없을 수 있음 (0 또는 1)
 */
@Entity
@Table(
        name = "user_password_credentials",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_password_login_id",
                        columnNames = "login_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPasswordCredentials {

    /**
     * PK이자 FK
     * - users.id 를 그대로 PK로 사용
     * - @MapsId와 함께 사용됨
     */
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long id;

    /**
     * User 엔티티와 1:1 연관관계
     * - PK 공유 (Shared Primary Key)
     * - user가 없으면 credentials도 존재할 수 없음
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 로그인 ID
     * - 이메일과 동일하게 쓸 수도 있고, 별도 ID로 쓸 수도 있음
     * - DB에서 UNIQUE 제약 (uq_user_password_login_id)
     */
    @Column(name = "login_id", nullable = false, length = 320)
    private String loginId;

    /**
     * 비밀번호 해시
     * - bcrypt 등 해시된 값
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * 비밀번호 마지막 변경 시각
     */
    @Column(name = "password_changed_at", nullable = false)
    private LocalDateTime passwordChangedAt;

    // == 비즈니스 로직 메서드 예시 ==

    /**
     * 비밀번호 변경
     * @param encodedPassword 이미 인코딩(bcrypt 등)된 비밀번호
     */
    public void changePassword(String encodedPassword) {
        this.passwordHash = encodedPassword;
        this.passwordChangedAt = LocalDateTime.now();
    }

    /**
     * 로그인 ID 변경
     */
    public void changeLoginId(String newLoginId) {
        this.loginId = newLoginId;
    }

    public static UserPasswordCredentials create(User user, String loginId, String encodedPassword) {
        return UserPasswordCredentials.builder()
                .user(user)                 // @MapsId 때문에 user.id 가 곧 이 엔티티의 id
                .loginId(loginId)
                .passwordHash(encodedPassword)
                .passwordChangedAt(LocalDateTime.now())
                .build();
    }
}
