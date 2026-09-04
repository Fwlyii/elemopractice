package com.tju.elm_bk.security;

import com.tju.elm_bk.controller.AdminController;
import com.tju.elm_bk.controller.FileUploadController;
import com.tju.elm_bk.controller.UserRestController;
import com.tju.elm_bk.dto.FoodCreateDTO;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.AssetMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.impl.AssetServiceImpl;
import com.tju.elm_bk.websocket.WebSocketServer;
import jakarta.websocket.Session;
import jakarta.websocket.CloseReason;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
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
    void uploadRejectsSpoofedMimeTypeUsingActualFileSignature() {
        FileUploadController controller = new FileUploadController();
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
        AssetServiceImpl service = new AssetServiceImpl(mock(AssetMapper.class), mock(UserMapper.class));
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
}
