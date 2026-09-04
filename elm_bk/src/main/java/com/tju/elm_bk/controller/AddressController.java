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

    @PostMapping("/me")
    @Operation(summary = "为当前用户新增地址")
    public HttpResult<AddressVO> addCurrentUserAddress(@Valid @RequestBody AddressCreateDTO createDTO) {
        createDTO.setCustomer(null);
        return addressService.addDeliveryAddress(createDTO);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户的地址列表")
    public HttpResult<List<DeliveryAddress>> listCurrentUserAddresses() {
        return addressService.listDeliveryAddressByUserId(null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取当前用户的指定地址")
    public HttpResult<DeliveryAddress> getAddress(@PathVariable Long id) {
        return addressService.getDeliveryAddressById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新当前用户的指定地址")
    public HttpResult updateAddress(@PathVariable Long id, @RequestBody DeliveryAddress deliveryAddress) {
        deliveryAddress.setId(id);
        return addressService.updateDeliveryAddress(deliveryAddress);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除当前用户的指定地址")
    public HttpResult deleteAddress(@PathVariable Long id) {
        DeliveryAddress address = new DeliveryAddress();
        address.setId(id);
        return addressService.deleteDeliveryAddress(address);
    }

    @Operation(summary = "获取用户地址列表")
    @GetMapping("/listDeliveryAddressByUserId")
    public HttpResult<List<DeliveryAddress>> listDeliveryAddressByUserId(@RequestParam(required = false) Long userId)
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
