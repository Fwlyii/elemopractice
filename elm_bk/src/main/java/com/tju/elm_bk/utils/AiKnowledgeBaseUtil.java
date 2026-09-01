package com.tju.elm_bk.utils;

import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.entity.Order;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.BusinessMapper;
import com.tju.elm_bk.mapper.FoodMapper;
import com.tju.elm_bk.mapper.OrdersMapper;
import com.tju.elm_bk.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiKnowledgeBaseUtil {
    
    private final BusinessMapper businessMapper;
    private final FoodMapper foodMapper;
    private final OrdersMapper ordersMapper;
    private final UserMapper userMapper;
    
    /**
     * 构建AI对话的系统提示词
     */
    public String buildSystemPrompt() {
        return """
                你是一个专业的外卖平台智能客服助手，名字叫"小饿"。你的主要职责是：
                
                1. 回答用户关于外卖订餐的各种问题
                2. 帮助用户查找商家和菜品信息
                3. 协助处理订单相关问题
                4. 提供贴心的用餐建议
                
                请注意：
                - 保持友善、专业的服务态度
                - 回答一定要准确、有用，基于实际的数据库信息
                - 如果无法确定答案，请诚实告知并建议联系人工客服
                - 回答要简洁明了，避免过于冗长
                - 可以适当使用emoji让对话更生动
                
                特别重要的订单查询处理原则：
                - 如果用户询问具体订单（如"查6号订单"），而我提供了具体的订单信息，请基于这些信息直接回答
                - 如果用户询问"我的订单"或"订单状态"而我提供了多个订单信息，请逐一介绍每个订单的状态
                - 优先使用我提供的实际订单数据，而不是给出通用的操作指导
                - 如果我提供了订单号、状态、金额等具体信息，请直接告诉用户这些信息
                - 当显示多个订单时，可以按状态分类介绍（如：进行中的订单、已完成的订单）
                - 只有在没有提供具体订单信息时，才给出查询方法的指导
                
                当前平台信息：
                - 平台名称：饿了吧外卖平台
                - 服务时间：7:00-23:00
                - 客服电话：400-888-8888
                - 咨询邮箱：elm_v2.0@elm.com
                """;
    }

    /**
     * 根据用户ID获取用户上下文信息（包含所有最近订单详情）
     */
    public Map<String, Object> getUserContext(Long userId) {
        Map<String, Object> context = new HashMap<>();

        try {
            // 获取用户基本信息
            User user = userMapper.findById(userId);
            if (user != null) {
                context.put("userId", userId);
                context.put("username", user.getUsername());
                context.put("userActivated", user.getActivated());
            }

            // 获取用户最近订单信息
            List<Order> recentOrders = ordersMapper.selectRecentOrdersByUserId(userId, 5);
            context.put("recentOrdersCount", recentOrders.size());

            // 存储所有订单的详细信息
            if (!recentOrders.isEmpty()) {
                List<Map<String, Object>> orderDetails = new ArrayList<>();

                for (int i = 0; i < recentOrders.size(); i++) {
                    Order order = recentOrders.get(i);
                    Map<String, Object> orderInfo = new HashMap<>();
                    orderInfo.put("orderIndex", i + 1); // 订单序号（从1开始）
                    orderInfo.put("orderId", order.getId());
                    orderInfo.put("orderState", order.getOrderState());
                    orderInfo.put("orderTotal", order.getOrderTotal());
                    orderInfo.put("orderDate", order.getOrderDate()); // 假设有订单日期字段
                    orderInfo.put("isLatest", i == 0); // 标记是否是最新订单

                    orderDetails.add(orderInfo);
                }

                context.put("recentOrders", orderDetails);

                // 同时保留最新订单的快速访问（可选）
                Order lastOrder = recentOrders.get(0);
                context.put("lastOrderId", lastOrder.getId());
                context.put("lastOrderState", lastOrder.getOrderState());
                context.put("lastOrderTotal", lastOrder.getOrderTotal());
            }

        } catch (Exception e) {
            log.error("获取用户上下文信息失败: userId={}", userId, e);
        }

        return context;
    }

    /**
     * 搜索相关商家信息
     */
    public List<Business> searchBusinesses(String keyword, int limit) {
        try {
            return businessMapper.searchByKeyword(keyword, limit);
        } catch (Exception e) {
            log.error("搜索商家失败: keyword={}", keyword, e);
            return List.of();
        }
    }
    
    /**
     * 搜索相关菜品信息
     */
    public List<Food> searchFoods(String keyword, int limit) {
        try {
            return foodMapper.searchByKeyword(keyword, limit);
        } catch (Exception e) {
            log.error("搜索菜品失败: keyword={}", keyword, e);
            return List.of();
        }
    }
    
    /**
     * 根据订单ID获取订单详情
     */
    public Order getOrderById(Long orderId) {
        try {
            log.info("正在查询订单ID: {}", orderId);
            Order order = ordersMapper.selectById(orderId);
            log.info("查询订单结果: orderId={}, order={}", orderId, order);
            return order;
        } catch (Exception e) {
            log.error("获取订单详情失败: orderId={}", orderId, e);
            return null;
        }
    }
    
    /**
     * 根据用户ID获取最近的订单列表
     */
    public List<Order> getRecentOrdersByUserId(Long userId, int limit) {
        try {
            log.info("正在查询用户ID: {} 的最近 {} 个订单", userId, limit);
            List<Order> orders = ordersMapper.selectRecentOrdersByUserId(userId, limit);
            log.info("查询到用户{}的订单数量: {}", userId, orders.size());
            for (Order order : orders) {
                log.info("订单详情: ID={}, 状态={}, 总金额={}, 客户ID={}, 商家ID={}, 下单时间={}", 
                    order.getId(), order.getOrderState(), order.getOrderTotal(), 
                    order.getCustomerId(), order.getBusinessId(), order.getOrderDate());
            }
            return orders;
        } catch (Exception e) {
            log.error("获取用户订单列表失败: userId={}, limit={}", userId, limit, e);
            return List.of();
        }
    }
    
    /**
     * 构建商家信息的文本描述
     */
    public String formatBusinessInfo(Business business) {
        if (business == null) return "";
        
        return String.format("商家【%s】，地址：%s，起送价：%.2f元，配送费：%.2f元，介绍：%s",
                business.getBusinessName(),
                business.getBusinessAddress(),
                business.getStartPrice(),
                business.getDeliveryPrice(),
                business.getBusinessExplain());
    }
    
    /**
     * 构建菜品信息的文本描述
     */
    public String formatFoodInfo(Food food) {
        if (food == null) return "";
        
        return String.format("菜品【%s】，价格：%.2f元，描述：%s",
                food.getFoodName(),
                food.getFoodPrice(),
                food.getFoodExplain());
    }
    
    /**
     * 构建订单信息的文本描述
     */
    public String formatOrderInfo(Order order) {
        if (order == null) return "";
        
        String stateText = switch (order.getOrderState()) {
            case 0 -> "待支付 💰";
            case 1 -> "已支付 ✅";
            case 2 -> "配送中 🚚";
            case 3 -> "已完成 ✨";
            case 4 -> "已取消 ❌";
            default -> "未知状态 ❓";
        };
        
        StringBuilder info = new StringBuilder();
        info.append(String.format("订单号：%d，状态：%s，总金额：%.2f元", 
                order.getId(), stateText, order.getOrderTotal().doubleValue()));
        
        if (order.getOrderDate() != null) {
            info.append(String.format("，下单时间：%s", order.getOrderDate()));
        }
        
        // 如果有商家信息，也显示出来
        if (order.getBusinessId() != null) {
            try {
                var business = businessMapper.selectById(order.getBusinessId());
                if (business != null) {
                    info.append(String.format("，商家：%s", business.getBusinessName()));
                }
            } catch (Exception e) {
                log.debug("获取订单商家信息失败: businessId={}", order.getBusinessId());
            }
        }
        
        return info.toString();
    }
}
