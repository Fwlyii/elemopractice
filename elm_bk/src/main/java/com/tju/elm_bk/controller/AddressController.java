package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.AddressCreateDTO;
import com.tju.elm_bk.entity.DeliveryAddress;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.AddressService;
import com.tju.elm_bk.vo.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@Tag(name="管理地址", description = "对配送地址的增删改查")
@RequiredArgsConstructor
public class AddressController {
    private final AddressService addressService;

    @PostMapping()
    @Operation(summary = "新增地址", description = "创建一个新的地址")
    public HttpResult<AddressVO> addDeliveryAddress(@Valid @RequestBody AddressCreateDTO createDTO) {
        return addressService.addDeliveryAddress(createDTO);
    }

    @Operation(summary = "获取用户地址列表")
    @GetMapping("/listDeliveryAddressByUserId")
    public HttpResult<List<DeliveryAddress>> listDeliveryAddressByUserId(Long userId)
    {
        return addressService.listDeliveryAddressByUserId(userId);
    }

    @Operation(summary = "根据配送地址id获取地址")
    @GetMapping("/getDeliveryAddressById")
    public HttpResult<DeliveryAddress> getDeliveryAddressById(Long id)
    {
        return addressService.getDeliveryAddressById(id);
    }

    @Operation(summary = "更新配送地址")
    @PostMapping("/updateDeliveryAddress")
    public HttpResult updateDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress)
    {
        return addressService.updateDeliveryAddress(deliveryAddress);
    }

    @Operation(summary = "删除配送地址")
    @PutMapping("/removeDeliveryAddress")
    public HttpResult removeDeliveryAddress(@RequestBody DeliveryAddress deliveryAddress)
    {
        return addressService.deleteDeliveryAddress(deliveryAddress);
    }
}
