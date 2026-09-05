package com.tju.elm_bk.security;

import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.service.LoginRolePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRolePolicyTest {

    private final LoginRolePolicy policy = new LoginRolePolicy();

    @Test
    void approvedRiderCannotLoginThroughCustomerPortal() {
        APIException error = assertThrows(APIException.class,
                () -> policy.decide(authentication("USER", "RIDER"), "user"));

        assertEquals("NOT_ENOUGH_PERMISSION", error.getCode());
        assertTrue(error.getMessage().contains("骑手端"));
    }

    @Test
    void approvedMerchantCannotLoginThroughCustomerPortal() {
        APIException error = assertThrows(APIException.class,
                () -> policy.decide(authentication("USER", "BUSINESS"), "user"));

        assertTrue(error.getMessage().contains("商家端"));
    }

    @Test
    void approvedRiderReceivesOnlyRiderSessionAuthority() {
        LoginRolePolicy.LoginDecision decision =
                policy.decide(authentication("USER", "RIDER"), "rider");

        assertEquals("rider", decision.portalKey());
        assertEquals("RIDER", decision.sessionAuthority());
        assertFalse(decision.applicationOnly());
    }

    @Test
    void pureCustomerMayEnterRiderPortalOnlyToApply() {
        LoginRolePolicy.LoginDecision decision = policy.decide(authentication("USER"), "rider");

        assertEquals("RIDER_APPLICANT", decision.sessionAuthority());
        assertTrue(decision.applicationOnly());
    }

    @Test
    void adminPortalHasHighestPriority() {
        assertThrows(APIException.class,
                () -> policy.decide(authentication("USER", "ADMIN"), "user"));
        assertEquals("ADMIN", policy.decide(authentication("USER", "ADMIN"), "admin")
                .sessionAuthority());
    }

    @Test
    void sessionBecomesInvalidWhenCandidateIsApproved() {
        assertTrue(LoginRolePolicy.isCurrentSession("rider", "RIDER_APPLICANT", List.of("USER")));
        assertFalse(LoginRolePolicy.isCurrentSession("rider", "RIDER_APPLICANT", List.of("USER", "RIDER")));
        assertTrue(LoginRolePolicy.isCurrentSession("rider", "RIDER", List.of("USER", "RIDER")));
    }

    private UsernamePasswordAuthenticationToken authentication(String... authorities) {
        var granted = Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken("demo", "unused", granted);
    }
}
