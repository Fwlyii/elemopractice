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
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.AddressVO;
import com.tju.elm_bk.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeliveryAddressMapper deliveryAddressMapper;
    @Override
    public HttpResult<AddressVO> addDeliveryAddress(AddressCreateDTO createDTO) {
        String currentUsername = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("当前用户未登录"));
        User targetUser = userMapper.findByUsernameWithAuthorities(createDTO.getCustomer().getUsername());
        User currentUser = userMapper.findByUsernameWithAuthorities(currentUsername);
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));
        if (currentUser == null) {
            throw new APIException("当前用户不存在");
        }
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
            address.setUserId(currentUser.getId());
            address.setUser(currentUser);

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
        String currentUsername = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("当前用户未登录"));
        User targetUser = userMapper.findByUserIdWithAuthorities(userId);
        User currentUser = userMapper.findByUsernameWithAuthorities(currentUsername);
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(auth -> "ADMIN".equals(auth.getName()));
        if (currentUser == null) {
            throw new APIException("当前用户不存在");
        }
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
        return HttpResult.success(deliveryAddressMapper.getDeliveryAddressById(id));
    }

    @Override
    public HttpResult updateDeliveryAddress(DeliveryAddress deliveryAddress) {
        String currentUsername = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("未获取到当前登录用户名"));
        User currentUser = userMapper.findByUsernameWithAuthorities(currentUsername);
        LocalDateTime now = LocalDateTime.now();
        deliveryAddress.setUpdateTime(now);
        deliveryAddress.setUpdater(currentUser.getId());
        return HttpResult.success(deliveryAddressMapper.updateDeliveryAddress(deliveryAddress));
    }

    @Override
    public HttpResult deleteDeliveryAddress(DeliveryAddress deliveryAddress) {
        deliveryAddress.setIsDeleted(true);
        deliveryAddress.setUpdateTime(LocalDateTime.now());
        String currentUsername = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("未获取到当前登录用户名"));
        Long currentUserId = userMapper.getUserIdByUsername(currentUsername);
        deliveryAddress.setUpdater(currentUserId);
        return HttpResult.success(deliveryAddressMapper.updateDeliveryAddress(deliveryAddress));
    }
}
