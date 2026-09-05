package com.tju.elm_bk.security;

import com.tju.elm_bk.entity.Authority;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.service.LoginRolePolicy;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

@Component
public class TokenProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TokenProvider.class);
    private static final String AUTHORITIES_KEY = "auth";
    private static final String ISSUED_AT_MILLIS_KEY = "iat_ms";
    private static final String SESSION_ROLE_KEY = "session_role";

    private final SecretKey key;
    private final long tokenValidityInMilliseconds;
    private final long tokenValidityInMillisecondsForRememberMe;

    public TokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.token-validity-in-seconds}") long tokenValidityInSeconds,
            @Value("${jwt.token-validity-in-seconds-for-remember-me}") long tokenValidityInSecondsForRememberMe) {

        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityInMilliseconds = tokenValidityInSeconds * 1000;
        this.tokenValidityInMillisecondsForRememberMe = tokenValidityInSecondsForRememberMe * 1000;
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return buildToken(authentication.getName(), authorities, null, rememberMe);
    }

    public String createRoleBoundToken(Authentication authentication, boolean rememberMe, String sessionRole) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        if (authorities.isBlank() || authorities.contains(",")) {
            throw new IllegalArgumentException("角色会话必须且只能包含一个权限");
        }
        return buildToken(authentication.getName(), authorities, sessionRole, rememberMe);
    }

    private String buildToken(String subject, String authorities, String sessionRole, boolean rememberMe) {

        long now = (new Date()).getTime();
        Date validity;
        if (rememberMe) {
            validity = new Date(now + this.tokenValidityInMillisecondsForRememberMe);
        } else {
            validity = new Date(now + this.tokenValidityInMilliseconds);
        }

        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .claim(AUTHORITIES_KEY, authorities)
                .claim(ISSUED_AT_MILLIS_KEY, now)
                .setIssuedAt(new Date(now));
        if (StringUtils.hasText(sessionRole)) {
            builder.claim(SESSION_ROLE_KEY, sessionRole);
        }
        return builder
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .filter(auth -> !auth.trim().isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                claims.getSubject(),
                "",
                true,
                true,
                true,
                true,
                authorities
        );

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            LOG.info("Invalid JWT token.");
            LOG.trace("Invalid JWT token trace.", e);
        }
        return false;
    }

    /**
     * 账号密码、状态或权限发生变化后，之前签发的令牌必须失效。
     * 缺少毫秒签发时间的历史令牌也默认失效，以免部署升级后继续绕过新规则。
     */
    public boolean isCurrentForAccount(String token, LocalDateTime accountUpdatedAt) {
        if (accountUpdatedAt == null) return true;
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            Number issuedAtMillis = claims.get(ISSUED_AT_MILLIS_KEY, Number.class);
            if (issuedAtMillis == null) return false;
            long accountUpdatedMillis = accountUpdatedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            return issuedAtMillis.longValue() >= accountUpdatedMillis;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isRoleBoundAndCurrentForAccount(String token, User account) {
        if (account == null || account.getAuthorities() == null) return false;
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody();
            String sessionRole = claims.get(SESSION_ROLE_KEY, String.class);
            String authorityClaim = claims.get(AUTHORITIES_KEY, String.class);
            if (!StringUtils.hasText(sessionRole) || !StringUtils.hasText(authorityClaim)
                    || authorityClaim.contains(",")) return false;
            List<String> currentAuthorities = account.getAuthorities().stream()
                    .filter(Objects::nonNull)
                    .map(Authority::getName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            return LoginRolePolicy.isCurrentSession(
                    sessionRole, authorityClaim, currentAuthorities);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
