package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.constant.MessageConstant;
import com.tju.elm_bk.dto.AddressCreateDTO;
import com.tju.elm_bk.entity.DeliveryAddress;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.DeliveryAddressMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.AddressService;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.vo.AddressVO;
import com.tju.elm_bk.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final UserMapper userMapper;
    private final DeliveryAddressMapper deliveryAddressMapper;
    private final CurrentUserService currentUserService;
    @Override
    public HttpResult<AddressVO> addDeliveryAddress(AddressCreateDTO createDTO) {
        if (createDTO == null || createDTO.getCustomer() == null
                || createDTO.getCustomer().getUsername() == null
                || createDTO.getCustomer().getUsername().isBlank()
                || createDTO.getContactName() == null || createDTO.getContactName().isBlank()
                || createDTO.getContactName().trim().length() > 40
                || createDTO.getContactTel() == null || !createDTO.getContactTel().matches("^1[3-9]\\d{9}$")
                || createDTO.getAddress() == null || createDTO.getAddress().isBlank() || createDTO.getAddress().trim().length() > 255
                || createDTO.getContactSex() == null
                || (createDTO.getContactSex() != 0 && createDTO.getContactSex() != 1)) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        User targetUser = userMapper.findByUsernameWithAuthorities(createDTO.getCustomer().getUsername());
        User currentUser = currentUserService.requireUser();
        boolean isAdmin = currentUserService.isAdmin(currentUser);
        if (targetUser == null) {
            throw  new APIException("目标用户不存在");
        }

        // 检查权限：只能新增自己的地址，或者管理员可以新增任何人的地址
        if (currentUser.getUsername().equals(targetUser.getUsername()) || isAdmin) {
            LocalDateTime now = LocalDateTime.now();
            DeliveryAddress address = new DeliveryAddress();
            BeanUtils.copyProperties(createDTO, address);
            address.setCreateTime(now);
            address.setUpdateTime(now);
            address.setCreator(currentUser.getId());
            address.setUpdater(currentUser.getId());
            address.setIsDeleted(false);
            // 管理员可以代目标用户创建地址，但地址归属必须是目标用户，而不是操作者。
            address.setUserId(targetUser.getId());
            address.setUser(targetUser);

            deliveryAddressMapper.insert(address);
            AddressVO addressVO = new AddressVO();
            BeanUtils.copyProperties(address, addressVO);
            UserVO userVO = new UserVO();
            if (address.getUser() != null) {
                BeanUtils.copyProperties(address.getUser(), userVO);
            }
            addressVO.setCustomer(userVO);
            return HttpResult.success(addressVO);
        }else {
            return HttpResult.failure(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }
    }

    @Override
    public HttpResult<List<DeliveryAddress>> listDeliveryAddressByUserId(Long userId) {
        User targetUser = userMapper.findByUserIdWithAuthorities(userId);
        User currentUser = currentUserService.requireUser();
        boolean isAdmin = currentUserService.isAdmin(currentUser);
        if (targetUser == null) {
            throw  new APIException("目标用户不存在");
        }
        if (currentUser.getUsername().equals(targetUser.getUsername()) || isAdmin){
            return HttpResult.success(deliveryAddressMapper.listDeliveryAddressByUserId(userId));
        }else {
            return HttpResult.failure(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
        }
    }

    @Override
    public HttpResult<DeliveryAddress> getDeliveryAddressById(Long id) {
        DeliveryAddress address = deliveryAddressMapper.getDeliveryAddressById(id);
        assertAddressOwner(address);
        return HttpResult.success(address);
    }

    @Override
    public HttpResult updateDeliveryAddress(DeliveryAddress deliveryAddress) {
        if (deliveryAddress == null || deliveryAddress.getId() == null) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        User currentUser = currentUser();
        DeliveryAddress existing = deliveryAddressMapper.getDeliveryAddressById(deliveryAddress.getId());
        assertAddressOwner(existing, currentUser);
        validateAddressFields(deliveryAddress);
        // 地址归属由数据库记录决定，忽略客户端伪造的 userId/creator/updater。
        deliveryAddress.setUserId(existing.getUserId());
        deliveryAddress.setCreator(existing.getCreator());
        LocalDateTime now = LocalDateTime.now();
        deliveryAddress.setUpdateTime(now);
        deliveryAddress.setUpdater(currentUser.getId());
        return HttpResult.success(deliveryAddressMapper.updateDeliveryAddress(deliveryAddress));
    }

    @Override
    public HttpResult deleteDeliveryAddress(DeliveryAddress deliveryAddress) {
        if (deliveryAddress == null || deliveryAddress.getId() == null) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        User currentUser = currentUser();
        DeliveryAddress existing = deliveryAddressMapper.getDeliveryAddressById(deliveryAddress.getId());
        assertAddressOwner(existing, currentUser);
        deliveryAddress.setIsDeleted(true);
        deliveryAddress.setUpdateTime(LocalDateTime.now());
        deliveryAddress.setUserId(existing.getUserId());
        deliveryAddress.setUpdater(currentUser.getId());
        return HttpResult.success(deliveryAddressMapper.updateDeliveryAddress(deliveryAddress));
    }

    private void validateAddressFields(DeliveryAddress address) {
        if (address.getContactName() == null || address.getContactName().isBlank() || address.getContactName().trim().length() > 40
                || address.getContactTel() == null || !address.getContactTel().matches("^1[3-9]\\d{9}$")
                || address.getAddress() == null || address.getAddress().isBlank() || address.getAddress().trim().length() > 255
                || address.getContactSex() == null
                || (address.getContactSex() != 0 && address.getContactSex() != 1)) {
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
    }

    private User currentUser() {
        return currentUserService.requireUser();
    }

    private void assertAddressOwner(DeliveryAddress address) {
        assertAddressOwner(address, currentUser());
    }

    private void assertAddressOwner(DeliveryAddress address, User currentUser) {
        if (address == null || Boolean.TRUE.equals(address.getIsDeleted())) {
            throw new APIException("地址不存在");
        }
        if (!currentUserService.isAdmin(currentUser)
                && !java.util.Objects.equals(address.getUserId(), currentUser.getId())) {
            throw new APIException(ResultCodeEnum.ADDRESS_PERMISSION_DENIED);
        }
    }
}
