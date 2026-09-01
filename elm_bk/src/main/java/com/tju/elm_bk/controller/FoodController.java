package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.FoodCreateDTO;
import com.tju.elm_bk.dto.FoodDTO;
import com.tju.elm_bk.dto.FoodUpdateDTO;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.FoodService;
import com.tju.elm_bk.vo.FoodItemVO;
import com.tju.elm_bk.vo.FoodVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@Tag(name="管理商品")
public class FoodController {

    @Autowired
    private FoodService foodService;

    @GetMapping
    @Operation(summary = "根据商家或订单获取商品列表",description = "老师测试用")
    public HttpResult<List<FoodVO>> getAllFoods(@RequestParam(required = false) Integer business, @RequestParam(required = false) Integer order) {
        return HttpResult.success(foodService.getFoodList(business,order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据商家或订单获取商品列表",description = "老师测试用")
    public HttpResult<FoodVO> getAllFoods(@PathVariable Long id) {
        return HttpResult.success(foodService.getFoodById(id));
    }

    @PostMapping
    @Operation(summary = "新增商品",description = "老师测试用")
    public HttpResult<FoodVO> addFood(@RequestBody FoodDTO foodDTO) {
        return HttpResult.success(foodService.addFood(foodDTO));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改商品信息",description = "老师测试用")
    public HttpResult<FoodVO> modifyFood(@RequestBody FoodDTO foodDTO,@PathVariable Long id) {
        return HttpResult.success(foodService.updateFood(foodDTO,id));
    }



    @GetMapping("/list")
    @Operation(summary = "根据商家获取商品列表",description = "普通用户只能看到已上架的")
    public HttpResult<List<FoodItemVO>> getAllFoods(@RequestParam Long businessId, @RequestParam(required = false) Integer shelveStatus) {
        return HttpResult.success(foodService.getFoodItemList(businessId, shelveStatus));
    }

    @PostMapping("/addItem")
    @Operation(summary = "(前端用这个)商铺新增商品",description = "管理员可以随便添，商家只能为自己的商铺添")
    //@PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<Long> addFoodItem(@RequestBody FoodCreateDTO foodCreateDTO) {
        return HttpResult.success(foodService.addFoodItem(foodCreateDTO));
    }

    @GetMapping("/status")
    @Operation(summary = "上架/下架商品",description = "shelveStatus 0-下架 1-上架")
    //@PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<Long> setFoodShelveStatus(@RequestParam Long foodId,@RequestParam Integer shelveStatus) {
        return HttpResult.success(foodService.setFoodStatus(foodId,shelveStatus));
    }

    @PostMapping("/modifyItem")
    @Operation(summary = "(前端用这个)商铺修改商品",description = "管理员可以随便改，商家只能为自己的商铺改")
    //@PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<Long> modifyFoodItem(@RequestBody FoodUpdateDTO foodUpdateDTO) {
        return HttpResult.success(foodService.modifyFoodMessage(foodUpdateDTO));
    }

    @GetMapping("/delete")
    @Operation(summary = "商家删除商品")
    //@PreAuthorize("hasAuthority('BUSINESS')")
    public HttpResult<Long> setFoodShelveStatus(@RequestParam Long foodId) {
        return HttpResult.success(foodService.deleteFood(foodId));
    }

}
