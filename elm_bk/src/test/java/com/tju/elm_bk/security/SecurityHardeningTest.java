package com.tju.elm_bk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.controller.AdminController;
import com.tju.elm_bk.controller.FileUploadController;
import com.tju.elm_bk.controller.UserRestController;
import com.tju.elm_bk.dto.FoodCreateDTO;
import com.tju.elm_bk.entity.Authority;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.ImageStorageService;
import com.tju.elm_bk.service.impl.AssetServiceImpl;
import com.tju.elm_bk.websocket.WebSocketServer;
import jakarta.websocket.Session;
import jakarta.websocket.CloseReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityHardeningTest {

    @Test
    void adminAndAccountStatusEndpointsRequireAdminAuthority() throws Exception {
        PreAuthorize adminGuard = AdminController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(adminGuard);
        assertEquals("hasAuthority('ADMIN')", adminGuard.value());

        Method toggle = UserRestController.class.getMethod("toggleUserStatus", String.class, Boolean.class);
        PreAuthorize toggleGuard = toggle.getAnnotation(PreAuthorize.class);
        assertNotNull(toggleGuard);
        assertEquals("hasAuthority('ADMIN')", toggleGuard.value());
    }

    @Test
    void uploadRejectsSpoofedMimeTypeUsingActualFileSignature(@TempDir Path tempDir) {
        FileUploadController controller = new FileUploadController(new ImageStorageService(tempDir.toString()));
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "payload.png", "image/png", "<script>alert(1)</script>".getBytes());

        assertThrows(APIException.class, () -> controller.uploadFile(fakeImage));
    }

    @Test
    void websocketRejectsConnectionWithoutAuthenticationToken() throws Exception {
        Session session = mock(Session.class);
        when(session.getRequestParameterMap()).thenReturn(Collections.emptyMap());

        new WebSocketServer().onOpen(session, "1");

        verify(session).close(org.mockito.ArgumentMatchers.any(CloseReason.class));
    }

    @Test
    void demoMoneyCreationIsDisabledByDefault() {
        AssetServiceImpl service = new AssetServiceImpl(
                mock(AssetMapper.class), mock(CurrentUserService.class));
        ReflectionTestUtils.setField(service, "demoEnabled", false);

        assertThrows(APIException.class, () -> service.recharge(BigDecimal.TEN));
        assertThrows(APIException.class, service::activateMembership);
    }

    @Test
    void unreasonableFoodValuesAreRejected() {
        FoodCreateDTO dto = new FoodCreateDTO();
        dto.setBusinessId(1L);
        dto.setFoodName("测试商品");
        dto.setFoodPrice(new BigDecimal("10.00"));
        dto.setStock(1_000_001);
        assertFalse(dto.verify());

        dto.setStock(10);
        dto.setFoodPrice(BigDecimal.ZERO);
        assertFalse(dto.verify());
    }

    @Test
    void accountChangeInvalidatesPreviouslyIssuedToken() {
        TokenProvider provider = new TokenProvider("a".repeat(128), 3600, 7200);
        var authentication = new UsernamePasswordAuthenticationToken(
                "demo_user", "unused", List.of(new SimpleGrantedAuthority("USER")));
        Instant beforeIssue = Instant.now();
        String token = provider.createToken(authentication, false);
        Instant afterIssue = Instant.now();
        ZoneId zone = ZoneId.systemDefault();

        assertTrue(provider.isCurrentForAccount(token,
                LocalDateTime.ofInstant(beforeIssue.minusSeconds(1), zone)));
        assertFalse(provider.isCurrentForAccount(token,
                LocalDateTime.ofInstant(afterIssue.plusSeconds(1), zone)));
    }

    @Test
    void roleBoundTokenCarriesOnlyTheSelectedPortalAuthority() {
        TokenProvider provider = new TokenProvider("a".repeat(128), 3600, 7200);
        var authentication = new UsernamePasswordAuthenticationToken(
                "demo_rider", "unused", List.of(new SimpleGrantedAuthority("RIDER")));
        String token = provider.createRoleBoundToken(authentication, false, "rider");
        User account = new User();
        Authority user = new Authority();
        user.setName("USER");
        Authority rider = new Authority();
        rider.setName("RIDER");
        account.setAuthorities(List.of(user, rider));

        assertEquals(List.of("RIDER"), provider.getAuthentication(token).getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList());
        assertTrue(provider.isRoleBoundAndCurrentForAccount(token, account));

        account.setAuthorities(List.of(user));
        assertFalse(provider.isRoleBoundAndCurrentForAccount(token, account));
    }

    @Test
    void securityHandlersKeepUnauthorizedAndForbiddenDistinct() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse unauthenticated = new MockHttpServletResponse();
        new JwtAuthenticationEntryPoint(objectMapper).commence(
                request, unauthenticated, new InsufficientAuthenticationException("missing"));
        assertEquals(401, unauthenticated.getStatus());

        MockHttpServletResponse forbidden = new MockHttpServletResponse();
        new JwtAccessDeniedHandler(objectMapper).handle(
                request, forbidden, new AccessDeniedException("denied"));
        assertEquals(403, forbidden.getStatus());
    }
}
