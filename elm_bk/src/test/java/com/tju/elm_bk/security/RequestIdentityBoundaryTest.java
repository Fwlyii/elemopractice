package com.tju.elm_bk.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.dto.AddressCreateDTO;
import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.dto.BusinessPermissionDTO;
import com.tju.elm_bk.dto.MerchantInteractionDTO;
import com.tju.elm_bk.entity.DeliveryAddress;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.DeliveryAddressMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestIdentityBoundaryTest {

    @Test
    void aiRequestCannotInjectAUserIdThroughJson() throws Exception {
        AiChatRequestDTO request = new ObjectMapper().readValue(
                "{\"message\":\"test\",\"chatType\":\"general\",\"userId\":999}",
                AiChatRequestDTO.class);

        assertNull(request.getUserId());
    }

    @Test
    void interactionRequestCannotInjectAUserIdThroughJson() throws Exception {
        MerchantInteractionDTO request = new ObjectMapper().readValue(
                "{\"userId\":999,\"merchantId\":1,\"liked\":true,\"collected\":false}",
                MerchantInteractionDTO.class);

        assertNull(request.getUserId());
        assertEquals(1L, request.getMerchantId());
    }

    @Test
    void shopApplicationCannotInjectAUserIdThroughJson() throws Exception {
        BusinessPermissionDTO request = new ObjectMapper().readValue(
                "{\"businessName\":\"test\",\"userId\":999}",
                BusinessPermissionDTO.class);

        assertNull(request.getUserId());
        assertEquals("test", request.getBusinessName());
    }

    @Test
    void addressWithoutClientIdentityBelongsToAuthenticatedUser() {
        UserMapper userMapper = mock(UserMapper.class);
        DeliveryAddressMapper addressMapper = mock(DeliveryAddressMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        AddressServiceImpl service = new AddressServiceImpl(userMapper, addressMapper, currentUserService);
        User currentUser = user(7L, "current-user");
        when(currentUserService.requireUser()).thenReturn(currentUser);
        when(currentUserService.isAdmin(currentUser)).thenReturn(false);

        service.addDeliveryAddress(validAddress());

        ArgumentCaptor<DeliveryAddress> captor = ArgumentCaptor.forClass(DeliveryAddress.class);
        verify(addressMapper).insert(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(7L, captor.getValue().getCreator());
    }

    @Test
    void normalUserCannotCreateAddressForAnotherUsername() {
        UserMapper userMapper = mock(UserMapper.class);
        DeliveryAddressMapper addressMapper = mock(DeliveryAddressMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        AddressServiceImpl service = new AddressServiceImpl(userMapper, addressMapper, currentUserService);
        User currentUser = user(7L, "current-user");
        when(currentUserService.requireUser()).thenReturn(currentUser);
        when(currentUserService.isAdmin(currentUser)).thenReturn(false);
        AddressCreateDTO request = validAddress();
        AddressCreateDTO.CustomerSimpleDTO customer = new AddressCreateDTO.CustomerSimpleDTO();
        customer.setUsername("another-user");
        request.setCustomer(customer);

        assertThrows(APIException.class, () -> service.addDeliveryAddress(request));
    }

    private AddressCreateDTO validAddress() {
        AddressCreateDTO dto = new AddressCreateDTO();
        dto.setContactName("张同学");
        dto.setContactSex(1);
        dto.setContactTel("13800138000");
        dto.setAddress("天津大学北洋园校区");
        return dto;
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
