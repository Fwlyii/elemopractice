package com.tju.elm_bk.service;

import com.tju.elm_bk.dto.AddressCreateDTO;
import com.tju.elm_bk.entity.DeliveryAddress;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.vo.AddressVO;
import jakarta.validation.Valid;

import java.util.List;

public interface AddressService {
    HttpResult<AddressVO> addDeliveryAddress(@Valid AddressCreateDTO createDTO);

    HttpResult<List<DeliveryAddress>> listDeliveryAddressByUserId(Long userId);

    HttpResult<DeliveryAddress> getDeliveryAddressById(Long id);

    HttpResult updateDeliveryAddress(DeliveryAddress deliveryAddress);

    HttpResult deleteDeliveryAddress(DeliveryAddress deliveryAddress);
}
