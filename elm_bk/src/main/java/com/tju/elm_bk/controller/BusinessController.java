package com.tju.elm_bk.controller;


import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessInfoDTO;
import com.tju.elm_bk.dto.BusinessUpdateDTO;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.BusinessService;
import com.tju.elm_bk.vo.BusinessSearchVO;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.MerchantStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
@Validated
@Tag(name="店铺管理", description = "提供店铺管理相关的接口")
public class BusinessController {

    @Autowired
    private final BusinessService businessService;

    /**
     * 根据ID获取店铺详情
     * @param id 店铺ID (路径参数)--牙膏1
     * @return 店铺详细信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "（牙膏）根据id获取某店铺详情", description = "根据id获取某店铺详情")
    public HttpResult<BusinessVO> getBusiness(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
//            log.warn("获取店铺详情请求参数错误: id={}", id);
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
        BusinessVO businessVo = businessService.getBusinessById(id);
//        if (businessVo == null) {
//            log.info("未找到对应的店铺信息: id={}", id);
//            throw new APIException(ResultCodeEnum.NOT_FOUND);
//        }
//        System.out.println("查询到的BusinessVO对象: " + businessVo);
//        log.debug("成功获取店铺详情: id={}, name={}", id, businessVo.getBusinessName());
        return HttpResult.success(businessVo);
    }

    /**
     * 更新店铺信息--牙膏
     * @param id 店铺ID (路径参数)
     * @param updateDto 更新数据
     * @return 更新后的店铺信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "（牙膏）更新某店铺信息（覆盖）", description = "更新某店铺信息")
    public HttpResult<BusinessVO> updateBusiness(@PathVariable("id") Long id,@RequestBody BusinessUpdateDTO updateDto){
        if (id == null || id <= 0) {
//            log.warn("更新店铺信息请求参数错误: id={}", id);
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }

        BusinessVO businessVo = businessService.updateBusiness(id, updateDto);
        return HttpResult.success(businessVo);
    }

     @DeleteMapping("/{id}")
     @Operation(summary = "（牙膏）删除某店铺")
    public HttpResult<BusinessVO> deleteBusiness(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            log.warn("删除店铺信息请求参数错误: id={}", id);
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
         BusinessVO businessVo = businessService.deleteBusiness(id);
        return HttpResult.success(businessVo);
    }

     @PatchMapping("/{id}")
     @Operation(summary = "（牙膏）部分更新某店铺信息")
    public HttpResult<BusinessVO> patchBusiness(@PathVariable("id") Long id,@RequestBody BusinessUpdateDTO updateDto) {
        if (id == null || id <= 0) {
            log.warn("更新店铺信息请求参数错误: id={}", id);
            throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
        }
         BusinessVO businessVo = businessService.patchBusiness(id,updateDto);
        return HttpResult.success(businessVo);
    }

    /**
     * 获取所有店铺信息
     * @return 所有店铺信息
     */
    @GetMapping
    @Operation(summary = "获取所有店铺信息--牙膏版本")
    public HttpResult<List<BusinessVO>> getBusinesses() {
        List<BusinessVO> businessVos = businessService.getBusinesses();
        return HttpResult.success(businessVos);
    }


     @PostMapping
     @Operation(summary = "添加新店铺——牙膏版本")
    public HttpResult<BusinessVO> addBusiness(@RequestBody BusinessDTO businessDTO) {
        BusinessVO businessVo = businessService.addBusiness(businessDTO);
        return HttpResult.success(businessVo);
    }



    @GetMapping("/search")
    @Operation(summary = "搜索店铺,可传入字符串keyword，isScore为0/1表示是否按照评分排序，isSales为0/1表示是否按照销售量排序")
    public HttpResult<List<BusinessSearchVO>> searchBusiness(
            @RequestParam(required = false)String  keyword,
            @RequestParam(required = false)boolean isScore,
            @RequestParam(required = false)boolean isSales){
        return HttpResult.success(businessService.getBusinessesBySearch(keyword,isScore,isSales));

    }

    @PostMapping("/apply")
    @Operation(summary = "(商家和管理员增加店铺)商家新添加店铺--带状态")
    public HttpResult<Integer> applyForAddBusiness(@RequestBody Business business) {
        return HttpResult.success(businessService.applyForAddBusiness(business));
    }

    /**
     * 查某商家的所有的店铺，可传状态
     * @return
     */

    @GetMapping("/merchant")
    @Operation(summary = "(返回需要的)查某商家的所有的店铺，可传状态(管理员商铺管理第二页)")
    public HttpResult<List<Business>> getMerchantBusinesses(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {
        List<Business> businesses = businessService.getMerchantBusinesses(userId, status);
        return HttpResult.success(businesses);
    }
    /**
     * 查所有激活的店铺
     * @return
     */
    @GetMapping("/active")
    @Operation(summary = "查所有激活的商家（管理端商铺管理第一页）")
    public List<BusinessInfoDTO> getAllActiveBusinesses() {
        return businessService.getAllActiveBusinesses();
    }


    /**
     * 通过类型查所有店铺
     */
    @GetMapping("/type")
    @Operation(summary = "通过类型查所有店铺")
    public HttpResult<List<Business>> listBusinessByOrderTypeId(@RequestParam(required = false) Integer type) {
        List<Business> businesses = businessService.listBusinessByOrderTypeId(type);
        return HttpResult.success(businesses);
    }

    @GetMapping("/id_list")
    @Operation(summary = "获取当前商家用户商铺id列表")
    public HttpResult<List<MerchantStatsVO>> getBusinessIdList() {
        return HttpResult.success(businessService.getBusinessIdList());
    }



    @GetMapping("/carousel")
    @Operation(summary = "获取轮播图商家")
    public HttpResult<List<BusinessSearchVO>> searchBusiness(){
        return HttpResult.success(businessService.getBusinessesInCarousel());

    }

    //还是更新商铺的接口，但部分跟新且适配数据库版本
    @PatchMapping("/own/{id}")
    @Operation(summary = "（可用）部分更新某店铺信息")
    public HttpResult<BusinessVO> patchBusinessOwn(@PathVariable("id") Long id,@RequestBody BusinessUpdateDTO updateDto) {
         BusinessVO businessVO = businessService.patchBusinessOwn( id,  updateDto);
         return HttpResult.success(businessVO);
    }


}