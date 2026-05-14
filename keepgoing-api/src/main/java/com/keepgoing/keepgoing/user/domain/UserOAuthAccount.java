package com.keepgoing.keepgoing.user.domain;

import com.keepgoing.keepgoing.global.common.entity.BaseEntity;
import com.keepgoing.keepgoing.user.domain.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * OAuth 계정 엔티티
 * - 하나의 User에 여러 OAuth 계정이 연결될 수 있음 (1 : N)
 * - (provider, providerUserId) 조합은 전역에서 유일해야 함
 */
@Entity
@Table(
        name = "user_oauth_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_user_oauth_provider_user",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserOAuthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * 어떤 User에 속한 OAuth 계정인지
     * - 한 User에 여러 OAuth 계정이 연결될 수 있음
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * OAuth 제공자 (GOOGLE, GITHUB, KAKAO ...)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private OAuthProvider provider;

    /**
     * Provider 내부에서 유저를 식별하는 ID
     * - Google: sub
     * - GitHub: id
     * - Kakao: id
     */
    @Column(name = "provider_user_id", nullable = false, length = 190)
    private String providerUserId;

    /**
     * Provider가 내려준 이메일
     * - User.email과 다를 수 있음
     * - null 허용 (이메일 제공 안 하는 provider 대비)
     */
    @Column(name = "email", length = 320)
    private String email;

    /**
     * Provider가 내려준 프로필 이미지 URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    // == 연관관계 편의 메서드 ==

    public void attachTo(User user) {
        this.user = user;
    }

    // == 프로필 정보 갱신 예시 ==

    public void updateProfile(String email, String avatarUrl) {
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    // 정적 팩토리로 생성할 때 쓸 수 있는 메서드 예시
    public static UserOAuthAccount create(
            User user,
            OAuthProvider provider,
            String providerUserId,
            String email,
            String avatarUrl
    ) {
        return UserOAuthAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(providerUserId)
                .email(email)
                .avatarUrl(avatarUrl)
                .build();
    }
}
