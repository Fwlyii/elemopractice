package com.tju.elm_bk.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.tju.elm_bk.service.UserModelDetailsService;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JWTFilter.class);
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final TokenProvider tokenProvider;
    private final UserModelDetailsService userDetailsService;
    private final UserMapper userMapper;

    public JWTFilter(TokenProvider tokenProvider, UserModelDetailsService userDetailsService, UserMapper userMapper) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/ws/")) {
            filterChain.doFilter(request, response);
            return;
        }
        LOG.debug("收到请求 {}", uri);
        try {
            String jwt = resolveToken(request);
            String requestURI = request.getRequestURI();

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Authentication tokenAuthentication = tokenProvider.getAuthentication(jwt);
                // JWT 只证明令牌曾被签发；账号是否仍启用、是否已删除以及当前权限
                // 必须以数据库中的最新状态为准，避免禁用账号或撤权后旧令牌继续生效。
                User account = userMapper.findByUsernameWithAuthorities(tokenAuthentication.getName());
                if (account == null || !Boolean.TRUE.equals(account.getActivated())
                        || !tokenProvider.isCurrentForAccount(jwt, account.getUpdateTime())
                        || !tokenProvider.isRoleBoundAndCurrentForAccount(jwt, account)) {
                    throw new IllegalArgumentException("账号状态已变化，请重新登录");
                }
                UserDetails currentUser = userDetailsService.loadUserByUsername(tokenAuthentication.getName());
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        currentUser, jwt, tokenAuthentication.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                LOG.debug("set Authentication to security context for '{}', uri: {}", authentication.getName(), requestURI);
            } else {
                LOG.debug("no valid JWT token found, uri: {}", requestURI);
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            LOG.info("JWT对应账号不存在、已禁用或令牌无效: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
