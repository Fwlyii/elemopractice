package com.tju.elm_bk.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tju.elm_bk.dto.AuthenticationDTO;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.security.TokenProvider;
import com.tju.elm_bk.service.LoginRolePolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "认证管理", description = "提供JWT认证相关接口")
@RequiredArgsConstructor
public class AuthenticationRestController {
    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final LoginRolePolicy loginRolePolicy;

    @PostMapping("/auth")
    @Operation(description = "身份认证成功后获取令牌")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody AuthenticationDTO loginDto) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
            LoginRolePolicy.LoginDecision decision = loginRolePolicy.decide(authentication, loginDto.getRole());
            Authentication sessionAuthentication = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(), authentication.getCredentials(),
                    List.of(new SimpleGrantedAuthority(decision.sessionAuthority())));
            SecurityContextHolder.getContext().setAuthentication(sessionAuthentication);

            boolean rememberMe = Boolean.TRUE.equals(loginDto.getRememberMe());
            String jwt = tokenProvider.createRoleBoundToken(
                    sessionAuthentication, rememberMe, decision.portalKey());

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Authorization", "Bearer " + jwt);

            return new ResponseEntity<>(new JWTToken(jwt, decision.portalKey(), decision.applicationOnly()),
                    httpHeaders, HttpStatus.OK);
        } catch (BadCredentialsException e) {
            throw new APIException("用户名或者密码错误");
        } catch (AuthenticationException e) {
            throw new APIException("用户未激活或账号锁定");
        }
    }

    public record JWTToken(
            @JsonProperty("id_token") String idToken,
            String role,
            @JsonProperty("application_only") boolean applicationOnly) {
    }
}
