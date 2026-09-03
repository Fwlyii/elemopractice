package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.result.ResultCodeEnum;
import com.tju.elm_bk.service.AiChatService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.AiChatHistoryVO;
import com.tju.elm_bk.vo.AiChatResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.PreferenceMapper;
import com.tju.elm_bk.mapper.AiChatHistoryMapper;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.vo.AiRecommendationVO;
import java.math.BigDecimal;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
@Validated
@Tag(name = "AI智能客服", description = "提供AI智能客服对话相关的接口")
public class AiChatController {
    
    private final AiChatService aiChatService;
    private final UserMapper userMapper;
    private final OrdersMapper ordersMapper;
    private final FoodMapper foodMapper;
    private final BusinessMapper businessMapper;
    private final PreferenceMapper preferenceMapper;
    private final AiChatHistoryMapper chatHistoryMapper;
    
    /**
     * 获取当前用户ID的辅助方法
     */
    private Long getCurrentUserId() {
        try {
            String username = SecurityUtils.getCurrentUsername()
                    .orElseThrow(() -> new APIException(ResultCodeEnum.UNAUTHORIZED));
            User currentUser = userMapper.findByUsername(username);
            return currentUser != null ? currentUser.getId() : null;
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * AI聊天接口
     */
    @PostMapping
    @Operation(summary = "发送消息给AI客服", description = "用户发送消息给AI客服，获取智能回复")
    public HttpResult<AiChatResponseVO> chat(@Valid @RequestBody AiChatRequestDTO request) {
        try {
            // 账号上下文优先于请求体中的 userId，避免伪造身份读取或写入他人会话。
            Long currentUserId = getCurrentUserId();
            if (currentUserId != null) {
                request.setUserId(currentUserId);
                if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
                    Long sessionUserId = chatHistoryMapper.findUserIdBySessionId(request.getSessionId());
                    if (sessionUserId != null && !currentUserId.equals(sessionUserId) && !isAdmin()) {
                        throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
                    }
                }
            } else if (request.getUserId() != null) {
                request.setUserId(null);
            }
            
            log.info("AI聊天请求: userId={}, message={}, chatType={}", 
                    request.getUserId(), request.getMessage(), request.getChatType());
            
            AiChatResponseVO response = aiChatService.chat(request);
            
            log.info("AI聊天响应: sessionId={}, processingTime={}ms", 
                    response.getSessionId(), response.getProcessingTime());
            
            return HttpResult.success(response);
            
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI聊天处理失败", e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }
    
    /**
     * 获取用户对话历史
     */
    @GetMapping("/history")
    @Operation(summary = "获取用户对话历史", description = "分页获取用户的AI对话历史记录")
    public HttpResult<List<AiChatHistoryVO>> getChatHistory(
            @Parameter(description = "用户ID，不传则使用当前登录用户") 
            @RequestParam(required = false) Long userId,
            @Parameter(description = "页码，从1开始") 
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小，最大50") 
            @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                throw new APIException(ResultCodeEnum.UNAUTHORIZED);
            }
            if (userId == null) {
                userId = currentUserId;
            }
            if (!currentUserId.equals(userId) && !isAdmin()) {
                throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
            }
            
            List<AiChatHistoryVO> history = aiChatService.getChatHistory(userId, page, size);
            return HttpResult.success(history);
            
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取对话历史失败", e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }
    
    /**
     * 根据会话ID获取对话历史
     */
    @GetMapping("/history/session/{sessionId}")
    @Operation(summary = "根据会话ID获取对话历史", description = "获取指定会话的完整对话历史")
    public HttpResult<List<AiChatHistoryVO>> getChatHistoryBySession(
            @Parameter(description = "会话ID") 
            @PathVariable String sessionId) {
        
        try {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }
            Long currentUserId = getCurrentUserId();
            Long sessionUserId = chatHistoryMapper.findUserIdBySessionId(sessionId);
            if (currentUserId == null || sessionUserId == null || (!currentUserId.equals(sessionUserId) && !isAdmin())) {
                throw new APIException(ResultCodeEnum.NOT_ENOUGH_PERMISSION);
            }
            List<AiChatHistoryVO> history = aiChatService.getChatHistoryBySession(sessionId);
            return HttpResult.success(history);
            
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            log.error("根据会话ID获取对话历史失败", e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }
    
    /**
     * 删除对话历史
     */
    @DeleteMapping("/history/{historyId}")
    @Operation(summary = "删除对话历史", description = "删除指定的对话历史记录")
    public HttpResult<Boolean> deleteChatHistory(
            @Parameter(description = "对话历史ID") 
            @PathVariable Long historyId) {
        
        try {
            if (historyId == null || historyId <= 0) {
                throw new APIException(ResultCodeEnum.PARAM_NOT_MATCHED);
            }
            
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                throw new APIException(ResultCodeEnum.UNAUTHORIZED);
            }
            
            Boolean result = aiChatService.deleteChatHistory(historyId, currentUserId);
            return HttpResult.success(result);
            
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除对话历史失败", e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }
    
    /**
     * 清理用户的旧对话记录
     */
    @PostMapping("/history/clean")
    @Operation(summary = "清理旧对话记录", description = "清理用户的旧对话记录，保留最近的N条")
    public HttpResult<Boolean> cleanOldChatHistory(
            @Parameter(description = "保留的记录数量，默认50条") 
            @RequestParam(defaultValue = "50") Integer keepCount) {
        
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                throw new APIException(ResultCodeEnum.UNAUTHORIZED);
            }
            
            Boolean result = aiChatService.cleanOldChatHistory(currentUserId, keepCount);
            return HttpResult.success(result);
            
        } catch (APIException e) {
            throw e;
        } catch (Exception e) {
            log.error("清理旧对话记录失败", e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }
    
    /**
     * AI客服健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "AI客服健康检查", description = "检查AI客服系统状态")
    public HttpResult<String> healthCheck() {
        try {
            // 这里可以添加一些简单的健康检查逻辑
            return HttpResult.success("AI客服系统运行正常 🤖");
        } catch (Exception e) {
            log.error("AI客服健康检查失败", e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }

    /** 结构化智能点餐：推荐结果来自在售菜品，前端可直接加入购物车，不把模型文本当作价格或商品事实。 */
    @GetMapping("/recommendations")
    @Operation(summary = "结构化智能点餐推荐")
    public HttpResult<List<AiRecommendationVO>> recommendations(
        @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) BigDecimal budget) {
        String keyword = query == null ? "" : query.trim();
        User preferenceUser = currentUserOrNull();
        if (keyword.isEmpty() && preferenceUser != null) {
            var preference = preferenceMapper.findByUserId(preferenceUser.getId());
            if (preference != null && preference.getTasteTags() != null && !preference.getTasteTags().isBlank()) {
                keyword = preference.getTasteTags().split("[,，\\s]+", 2)[0];
            } else if (preference != null && preference.getCategoryTags() != null && !preference.getCategoryTags().isBlank()) {
                keyword = preference.getCategoryTags().split("[,，\\s]+", 2)[0];
            }
        }
        List<Food> foods = foodMapper.searchByKeyword(keyword, 20);
        if (preferenceUser != null) {
            var preference = preferenceMapper.findByUserId(preferenceUser.getId());
            if (preference != null && preference.getAvoidTags() != null && !preference.getAvoidTags().isBlank()) {
                var avoid = java.util.Arrays.stream(preference.getAvoidTags().split("[,，\\s]+"))
                        .filter(s -> !s.isBlank()).toList();
                foods = foods.stream().filter(f -> avoid.stream().noneMatch(tag ->
                        (f.getFoodName() != null && f.getFoodName().contains(tag)) ||
                        (f.getFoodExplain() != null && f.getFoodExplain().contains(tag)))).toList();
            }
        }
        if (budget != null && budget.compareTo(BigDecimal.ZERO) > 0) foods = foods.stream().filter(f -> f.getFoodPrice() != null && f.getFoodPrice().compareTo(budget) <= 0).toList();
        List<AiRecommendationVO> result = new ArrayList<>();
        for (Food food : foods.stream().limit(6).toList()) {
            Business b = businessMapper.selectBusinessById(food.getBusinessId());
            result.add(new AiRecommendationVO(food.getId(), food.getFoodName(), food.getFoodPrice(), food.getFoodImg(), food.getBusinessId(), b == null ? "" : b.getBusinessName(), "根据在售菜品与预算匹配"));
        }
        return HttpResult.success(result);
    }

    private boolean isAdmin() {
        try {
            User u = userMapper.findByUsernameWithAuthorities(SecurityUtils.getCurrentUsername().orElse(""));
            return u != null && u.getAuthorities() != null
                    && u.getAuthorities().stream().anyMatch(a -> "ADMIN".equals(a.getName()));
        } catch (Exception ignored) { return false; }
    }

    private User currentUserOrNull() {
        try { return userMapper.findByUsername(SecurityUtils.getCurrentUsername().orElse("")); }
        catch (Exception ignored) { return null; }
    }
    
    /**
     * 调试接口：查看用户订单数据
     */
    @GetMapping("/debug/orders/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "调试：查看用户所有订单（测试用）", description = "仅用于调试订单查询问题")
    public HttpResult<Map<String, Object>> debugUserOrders(@PathVariable Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            log.info("=== 开始调试用户{}的订单数据 ===", userId);
            
            // 1. 查询用户信息
            User user = userMapper.findById(userId);
            result.put("user", user);
            log.info("用户信息: {}", user);
            
            // 2. 查询所有订单（不限制条数）
            List<Order> allOrders = ordersMapper.selectRecentOrdersByUserId(userId, 100);
            result.put("allOrdersCount", allOrders.size());
            result.put("allOrders", allOrders);
            log.info("用户{}总订单数: {}", userId, allOrders.size());
            
            // 3. 查询最近5个订单
            List<Order> recentOrders = ordersMapper.selectRecentOrdersByUserId(userId, 5);
            result.put("recentOrdersCount", recentOrders.size());
            result.put("recentOrders", recentOrders);
            log.info("用户{}最近5个订单数: {}", userId, recentOrders.size());
            
            // 4. 查询特定订单（如果存在）
            try {
                Order order6 = ordersMapper.selectById(6L);
                result.put("order6", order6);
                if (order6 != null) {
                    log.info("订单6详情: ID={}, 客户ID={}, 状态={}", order6.getId(), order6.getCustomerId(), order6.getOrderState());
                } else {
                    log.info("订单6不存在");
                }
            } catch (Exception e) {
                log.warn("查询订单6失败: {}", e.getMessage());
            }
            
            try {
                Order order11 = ordersMapper.selectById(11L);
                result.put("order11", order11);
                if (order11 != null) {
                    log.info("订单11详情: ID={}, 客户ID={}, 状态={}", order11.getId(), order11.getCustomerId(), order11.getOrderState());
                } else {
                    log.info("订单11不存在");
                }
            } catch (Exception e) {
                log.warn("查询订单11失败: {}", e.getMessage());
            }
            
            // 5. 查询多个用户的订单用于对比
            try {
                log.info("尝试查询其他用户的订单进行对比分析");
                Map<Long, Integer> userOrderCount = new HashMap<>();
                
                // 查询用户1-5的订单数量
                for (long testUserId = 1; testUserId <= 5; testUserId++) {
                    List<Order> userOrders = ordersMapper.selectRecentOrdersByUserId(testUserId, 100);
                    userOrderCount.put(testUserId, userOrders.size());
                    log.info("用户{}的订单数量: {}", testUserId, userOrders.size());
                }
                
                result.put("userOrderCount", userOrderCount);
                log.info("用户订单统计: {}", userOrderCount);
            } catch (Exception e) {
                log.warn("查询用户订单统计失败: {}", e.getMessage());
            }
            
            log.info("=== 调试结束 ===");
            
            return HttpResult.success(result);
            
        } catch (Exception e) {
            log.error("调试用户订单失败", e);
            throw new APIException(ResultCodeEnum.SERVER_ERROR);
        }
    }
}
