package com.keepgoing.keepgoing.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final String TEST_SECRET = "test-secret-key-for-testing-purpose-only-32bytes!!";
    private static final String DIFFERENT_SECRET = "different-secret-key-for-test-32bytes!!";
    private static final Long TEST_USER_ID = 1L;
    private static final List<String> TEST_ROLES = List.of("ROLE_USER");
    private static final String TEST_ISSUER = "test-issuer";
    private static final Long TEST_ACCESS_TOKEN_EXPIRY = 3_600_000L;
    private static final Long TEST_REFRESH_TOKEN_EXPIRY = 604_800_000L;

    private static JwtProvider jwtProvider;

    @BeforeAll
    static void setup() {
        JwtProperties properties = new JwtProperties(
                TEST_SECRET,
                TEST_ACCESS_TOKEN_EXPIRY,
                TEST_REFRESH_TOKEN_EXPIRY,
                TEST_ISSUER
        );
        jwtProvider = new JwtProvider(properties);
    }

    @Test
    @DisplayName("Access Token 생성 시 userId와 roles가 포함된 유효한 토큰을 반환한다.")
    void generateAccessToken_containsCorrectClaims() throws ParseException {
        // given & when
        String token = jwtProvider.generateAccessToken(TEST_USER_ID, TEST_ROLES);

        // then
        SignedJWT signedJwt = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();

        assertThat(claimsSet.getSubject()).isEqualTo(String.valueOf(TEST_USER_ID));
        assertThat(claimsSet.getClaim("roles")).isEqualTo(TEST_ROLES);
        assertThat(claimsSet.getClaim("type")).isEqualTo("ACCESS");
        assertThat(claimsSet.getIssuer()).isEqualTo(String.valueOf(TEST_ISSUER));
    }

    @Test
    @DisplayName("RefreshToken 생성 시 올바른 claims가 포함된 JWT를 반환한다.")
    void generateRefreshToken_containsCorrectClaims() throws ParseException {
        // given & when
        String token = jwtProvider.generateRefreshToken(TEST_USER_ID);

        // then
        SignedJWT signedJwt = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();

        assertThat(claimsSet.getSubject()).isEqualTo(String.valueOf(TEST_USER_ID));
        assertThat(claimsSet.getClaim("roles")).isNull();
        assertThat(claimsSet.getClaim("type")).isEqualTo("REFRESH");
        assertThat(claimsSet.getIssuer()).isEqualTo(String.valueOf(TEST_ISSUER));
    }

    @Test
    @DisplayName("AccessToken의 만료 시간이 설정된 값과 일치한다.")
    void generateAccessToken_hasCorrectExpiry() throws ParseException {
        // given
        long beforeGeneration = System.currentTimeMillis();

        // when
        String token = jwtProvider.generateAccessToken(TEST_USER_ID, TEST_ROLES);

        // then
        SignedJWT signedJwt = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();

        long actualExpiry = claimsSet.getExpirationTime().getTime();
        long expectedExpiry = beforeGeneration + TEST_ACCESS_TOKEN_EXPIRY;

        assertThat(actualExpiry).isCloseTo(expectedExpiry, within(1000L));
    }

    @Test
    @DisplayName("RefreshToken의 만료 시간이 설정된 값과 일치한다.")
    void generateRefreshToken_hasCorrectExpiry() throws ParseException {
        // given
        long beforeGeneration = System.currentTimeMillis();

        // when
        String token = jwtProvider.generateRefreshToken(TEST_USER_ID);

        // then
        SignedJWT signedJwt = SignedJWT.parse(token);
        JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();

        long actualExpiry = claimsSet.getExpirationTime().getTime();
        long expectedExpiry = beforeGeneration + TEST_REFRESH_TOKEN_EXPIRY;

        assertThat(actualExpiry).isCloseTo(expectedExpiry, within(1000L));
    }

    @Test
    @DisplayName("유효한 AccessToken 검증 시 올바른 Claims를 반환한다.")
    void validateAccessToken_withValidToken_returnsClaims() {
        // given
        String token = jwtProvider.generateAccessToken(TEST_USER_ID, TEST_ROLES);

        // when
        JWTClaimsSet claims = jwtProvider.validateAccessTokenAndGetClaims(token);

        // then
        assertThat(claims.getSubject()).isEqualTo(String.valueOf(TEST_USER_ID));
    }

    @Test
    @DisplayName("유효한 RefreshToken 검증 시 올바른 Claims를 반환한다.")
    void validateRefreshToken_withValidToken_returnsClaims() {
        // given
        String token = jwtProvider.generateRefreshToken(TEST_USER_ID);

        // when
        Long userId = jwtProvider.validateRefreshTokenAndGetUserId(token);

        // then
        assertThat(userId).isEqualTo(TEST_USER_ID);
    }

    // === 검증 실패 테스트: 만료된 토큰 ===

    @Test
    @DisplayName("만료된 AccessToken 검증 시 AUTH_TOKEN_EXPIRED 예외가 발생한다.")
    void validateAccessToken_withExpiredToken_throwsExpired() throws Exception {
        // given - 이미 만료된 토큰 직접 생성
        // JwtProvider의 CLOCK_SKEW_SECONDS(120초)보다 더 과거로 설정해야 함
        String expiredToken = createTokenWithExpiry(
                TEST_USER_ID,
                TEST_ROLES,
                "ACCESS",
                -130_000L  // 130초 전에 만료 (clock skew 120초 초과)
        );

        // when & then
        assertThatThrownBy(() -> jwtProvider.validateAccessTokenAndGetClaims(expiredToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED);
                });
    }

    @Test
    @DisplayName("만료된 RefreshToken 검증 시 AUTH_REFRESH_TOKEN_EXPIRED 예외가 발생한다.")
    void validateRefreshToken_withExpiredToken_throwsExpired() throws Exception {
        // given - clock skew(120초)보다 더 과거로 설정
        String expiredToken = createTokenWithExpiry(
                TEST_USER_ID,
                null,
                "REFRESH",
                -130_000L  // 130초 전에 만료
        );

        // when & then
        assertThatThrownBy(() -> jwtProvider.validateRefreshTokenAndGetUserId(expiredToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
                });
    }

    // === 검증 실패 테스트: 잘못된 서명 ===

    @Test
    @DisplayName("다른 secret으로 생성된 토큰 검증 시 AUTH_TOKEN_INVALID 예외가 발생한다.")
    void validateAccessToken_withDifferentSecret_throwsInvalid() {
        // given - 다른 secret으로 서명된 토큰
        JwtProperties differentProps = new JwtProperties(
                DIFFERENT_SECRET,
                TEST_ACCESS_TOKEN_EXPIRY,
                TEST_REFRESH_TOKEN_EXPIRY,
                TEST_ISSUER
        );
        JwtProvider differentProvider = new JwtProvider(differentProps);
        String tokenFromDifferentSecret = differentProvider.generateAccessToken(TEST_USER_ID, TEST_ROLES);

        // when & then - 원래 provider로 검증 시 실패
        assertThatThrownBy(() -> jwtProvider.validateAccessTokenAndGetClaims(tokenFromDifferentSecret))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
                });
    }

    // === 검증 실패 테스트: 타입 불일치 ===

    @Test
    @DisplayName("RefreshToken으로 AccessToken 검증 시 AUTH_TOKEN_INVALID 예외가 발생한다.")
    void validateAccessToken_withRefreshToken_throwsInvalid() {
        // given
        String refreshToken = jwtProvider.generateRefreshToken(TEST_USER_ID);

        // when & then
        assertThatThrownBy(() -> jwtProvider.validateAccessTokenAndGetClaims(refreshToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
                });
    }

    @Test
    @DisplayName("AccessToken으로 RefreshToken 검증 시 AUTH_REFRESH_TOKEN_INVALID 예외가 발생한다.")
    void validateRefreshToken_withAccessToken_throwsInvalid() {
        // given
        String accessToken = jwtProvider.generateAccessToken(TEST_USER_ID, TEST_ROLES);

        // when & then
        assertThatThrownBy(() -> jwtProvider.validateRefreshTokenAndGetUserId(accessToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
                });
    }

    // === 검증 실패 테스트: 잘못된 issuer ===

    @Test
    @DisplayName("다른 issuer로 생성된 토큰 검증 시 AUTH_TOKEN_INVALID 예외가 발생한다.")
    void validateAccessToken_withDifferentIssuer_throwsInvalid() throws Exception {
        // given
        String tokenWithDifferentIssuer = createTokenWithIssuer(
                TEST_USER_ID,
                TEST_ROLES,
                "ACCESS",
                "different-issuer"
        );

        // when & then
        assertThatThrownBy(() -> jwtProvider.validateAccessTokenAndGetClaims(tokenWithDifferentIssuer))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
                });
    }

    // === 검증 실패 테스트: 잘못된 형식 ===

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 시 AUTH_TOKEN_INVALID 예외가 발생한다.")
    void validateAccessToken_withMalformedToken_throwsInvalid() {
        // given
        String malformedToken = "not.a.valid.jwt.token";

        // when & then
        assertThatThrownBy(() -> jwtProvider.validateAccessTokenAndGetClaims(malformedToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
                });
    }

    // === 헬퍼 메서드 ===

    /**
     * 테스트용 토큰 생성 - 만료 시간 지정 가능
     * JwtProvider로는 만료된 토큰을 생성할 수 없으므로 라이브러리 직접 사용
     */
    private String createTokenWithExpiry(Long userId, List<String> roles, String type, long expiryFromNow)
            throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim("type", type)
                .issuer(TEST_ISSUER)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + expiryFromNow));

        if (roles != null) {
            builder.claim("roles", roles);
        }

        return signWithTestSecret(builder.build());
    }

    /**
     * 테스트용 토큰 생성 - issuer 지정 가능
     * JwtProvider로는 다른 issuer를 가진 토큰을 생성할 수 없으므로 라이브러리 직접 사용
     */
    private String createTokenWithIssuer(Long userId, List<String> roles, String type, String issuer)
            throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim("type", type)
                .claim("roles", roles)
                .issuer(issuer)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + TEST_ACCESS_TOKEN_EXPIRY))
                .build();

        return signWithTestSecret(claims);
    }

    /**
     * TEST_SECRET으로 서명
     */
    private String signWithTestSecret(JWTClaimsSet claims) throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT signedJWT = new SignedJWT(header, claims);

        byte[] secretBytes = TEST_SECRET.getBytes(StandardCharsets.UTF_8);
        JWSSigner signer = new MACSigner(new SecretKeySpec(secretBytes, "HmacSHA256"));
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }
}