package com.tju.elm_bk.service;

import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.result.ResultCodeEnum;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 四端登录的唯一角色策略。数据库允许保留 USER 基础权限，但账号的工作端
 * 按 ADMIN > BUSINESS > RIDER > USER 确定，避免同一运营账号误入顾客端。
 */
@Service
public class LoginRolePolicy {

    public LoginDecision decide(Authentication authentication, String requestedRole) {
        Set<String> authorities = authorityNames(authentication.getAuthorities());
        Portal accountPortal = primaryPortal(authorities);
        Portal requestedPortal = Portal.fromKey(requestedRole);

        if (accountPortal == null) {
            throw denied("账号尚未配置可用身份，请联系管理员");
        }

        // 普通顾客可以从商家/骑手端登录并进入申请页；审核通过后，其主工作端
        // 会发生变化，此后便不能再从用户端登录。
        if (accountPortal == Portal.USER
                && (requestedPortal == Portal.MERCHANT || requestedPortal == Portal.RIDER)) {
            return new LoginDecision(requestedPortal.key, requestedPortal.applicantAuthority(), true);
        }

        if (accountPortal != requestedPortal) {
            throw denied("该账号属于" + accountPortal.label + "账号，请选择" + accountPortal.label + "端登录");
        }
        return new LoginDecision(requestedPortal.key, requestedPortal.authority, false);
    }

    /**
     * 校验令牌中的端和单一会话权限是否仍符合账号的最新角色。
     * 账号在审核通过或撤权后，旧端令牌会立即失效并要求重新登录。
     */
    public static boolean isCurrentSession(String portalKey, String sessionAuthority,
                                           Collection<String> currentAuthorities) {
        Set<String> authorities = currentAuthorities.stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
        Portal accountPortal = primaryPortal(authorities);
        Portal requestedPortal;
        try {
            requestedPortal = Portal.fromKey(portalKey);
        } catch (APIException ex) {
            return false;
        }
        if (accountPortal == Portal.USER
                && (requestedPortal == Portal.MERCHANT || requestedPortal == Portal.RIDER)) {
            return requestedPortal.applicantAuthority().equals(sessionAuthority);
        }
        return accountPortal == requestedPortal && requestedPortal.authority.equals(sessionAuthority);
    }

    private static Set<String> authorityNames(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    private static Portal primaryPortal(Set<String> authorities) {
        if (authorities.contains("ADMIN")) return Portal.ADMIN;
        if (authorities.contains("BUSINESS")) return Portal.MERCHANT;
        if (authorities.contains("RIDER")) return Portal.RIDER;
        if (authorities.contains("USER")) return Portal.USER;
        return null;
    }

    private APIException denied(String message) {
        return new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION.getCode(), message);
    }

    public record LoginDecision(String portalKey, String sessionAuthority, boolean applicationOnly) {}

    private enum Portal {
        USER("user", "用户", "USER"),
        MERCHANT("merchant", "商家", "BUSINESS"),
        RIDER("rider", "骑手", "RIDER"),
        ADMIN("admin", "管理员", "ADMIN");

        private final String key;
        private final String label;
        private final String authority;

        Portal(String key, String label, String authority) {
            this.key = key;
            this.label = label;
            this.authority = authority;
        }

        private String applicantAuthority() {
            return authority + "_APPLICANT";
        }

        private static Portal fromKey(String value) {
            if (value == null) throw new APIException(ResultCodeEnum.PARAM_VERIFIED_FAILED);
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Portal portal : values()) {
                if (portal.key.equals(normalized)) return portal;
            }
            throw new APIException(ResultCodeEnum.PARAM_VERIFIED_FAILED);
        }
    }
}
