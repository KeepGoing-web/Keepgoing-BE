package com.keepgoing.keepgoing.global.security.jwt;

import com.keepgoing.keepgoing.global.common.error.BusinessException;
import com.keepgoing.keepgoing.global.common.error.ErrorCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtProvider {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "ACCESS";
    private static final String TYPE_REFRESH = "REFRESH";

    private static final long CLOCK_SKEW_SECONDS = 120;
    private static final JWSHeader JWT_HEADER = new JWSHeader.Builder(JWSAlgorithm.HS256)
            .type(JOSEObjectType.JWT)
            .build();

    private final JwtProperties properties;
    private final JWSSigner signer;
    private final JWSVerifier verifier;

    public JwtProvider(JwtProperties properties) {
        this.properties = Objects.requireNonNull(properties, "JwtProperties must not be null");
        String secret = Objects.requireNonNull(properties.secretKey(), "JWT Secret must not be null");
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT 시크릿 키는 최소 32바이트(256bit) 이상이어야 합니다.");
        }

        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        try {
            this.signer = new MACSigner(secretKey);
            this.verifier = new MACVerifier(secretKey);
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT Signer/Verifier 초기화 실패: 시크릿 키 확인", e);
        }
    }

    public String generateAccessToken(Long userId, List<String> roles) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuer(properties.issuer())
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + properties.accessTokenExpiry()))
                .build();
        return signToken(claimsSet);
    }

    public String generateRefreshToken(Long userId) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .issuer(properties.issuer())
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + properties.refreshTokenExpiry()))
                .build();
        return signToken(claimsSet);
    }

    public List<String> getRolesFromToken(String token) {
        JWTClaimsSet claims = validateAccessTokenAndGetClaims(token);
        Object raw = claims.getClaim(CLAIM_ROLES);

        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(String::valueOf)
                .toList();
    }

    public Long getUserIdFromToken(String token) {
        JWTClaimsSet claims = validateAccessTokenAndGetClaims(token);

        return parseUserIdFromSubject(claims);
    }

    public JWTClaimsSet validateAccessTokenAndGetClaims(String token) {
        JWTClaimsSet claims = parseAndVerify(token, ErrorCode.AUTH_TOKEN_INVALID);

        validateExpiration(claims, ErrorCode.AUTH_TOKEN_EXPIRED);
        validateIssuer(claims, ErrorCode.AUTH_TOKEN_INVALID);
        validateType(claims, TYPE_ACCESS, ErrorCode.AUTH_TOKEN_INVALID);

        return claims;
    }

    public Long validateRefreshTokenAndGetUserId(String token) {
        JWTClaimsSet claims = parseAndVerify(token, ErrorCode.AUTH_REFRESH_TOKEN_INVALID);

        validateExpiration(claims, ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        validateIssuer(claims, ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        validateType(claims, TYPE_REFRESH, ErrorCode.AUTH_REFRESH_TOKEN_INVALID);

        return parseUserIdFromSubject(claims);
    }

    private static Long parseUserIdFromSubject(JWTClaimsSet claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            log.debug("토큰 Subject 파싱 실패: {}", claims.getSubject() );
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    private JWTClaimsSet parseAndVerify(String token, ErrorCode invalidCode) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                throw new BusinessException(invalidCode);
            }
            return signedJWT.getJWTClaimsSet();
        } catch (JOSEException | ParseException e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            throw new BusinessException(invalidCode);
        }
    }

    private void validateExpiration(JWTClaimsSet claims, ErrorCode expiredCode) {
        Date exp = claims.getExpirationTime();
        long skewMillis = CLOCK_SKEW_SECONDS * 1000L;
        Date nowMinusSkew = new Date(System.currentTimeMillis() - skewMillis);

        // 만료된 지 최대 CLOCK_SKEW_SECONDS 까지는 허용 (서버/클라이언트 시계 오차 대비)
        if (exp == null || exp.before(nowMinusSkew)) {
            throw new BusinessException(expiredCode);
        }
    }

    private void validateIssuer(JWTClaimsSet claims, ErrorCode invalidCode) {
        if (!properties.issuer().equals(claims.getIssuer())) {
            throw new BusinessException(invalidCode);
        }
    }

    private void validateType(JWTClaimsSet claims, String expectedType, ErrorCode invalidCode) {
        String type = (String) claims.getClaim(CLAIM_TYPE);
        if (!expectedType.equals(type)) {
            throw new BusinessException(invalidCode);
        }
    }

    private String signToken(JWTClaimsSet claimsSet) {
        try {
            SignedJWT signedJWT = new SignedJWT(JWT_HEADER, claimsSet);

            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("JWT 서명 실패", e);
        }
    }
}
