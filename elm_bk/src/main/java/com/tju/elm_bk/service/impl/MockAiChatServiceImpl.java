package com.tju.elm_bk.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.dto.AiChatRequestDTO;
import com.tju.elm_bk.entity.AiChatHistory;
import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.entity.Food;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.mapper.AiChatHistoryMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.service.AiChatService;
import com.tju.elm_bk.utils.AiKnowledgeBaseUtil;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.AiChatHistoryVO;
import com.tju.elm_bk.vo.AiChatResponseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Primary  // 优先使用这个实现
@RequiredArgsConstructor
public class MockAiChatServiceImpl implements AiChatService {
    
    private final AiKnowledgeBaseUtil knowledgeBaseUtil;
    private final AiChatHistoryMapper chatHistoryMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    
    /**
     * 获取当前用户ID的辅助方法
     */
    private Long getCurrentUserId() {
        try {
            String username = SecurityUtils.getCurrentUsername()
                    .orElse(null);
            if (username == null) return null;
            
            User currentUser = userMapper.findByUsername(username);
            return currentUser != null ? currentUser.getId() : null;
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    @Transactional
    public AiChatResponseVO chat(AiChatRequestDTO request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 生成或使用现有的会话ID
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
            
            // 生成模拟AI回复
            String aiMessage = generateMockResponse(request);
            
            // 构建响应
            AiChatResponseVO response = new AiChatResponseVO();
            response.setMessage(aiMessage);
            response.setSessionId(sessionId);
            response.setResponseType("text");
            response.setResponseTime(LocalDateTime.now());
            
            long processingTime = System.currentTimeMillis() - startTime;
            response.setProcessingTime(processingTime);
            
            // 保存对话历史
            saveChatHistory(request, response, processingTime);
            
            log.info("模拟AI回复生成成功: sessionId={}, processingTime={}ms", sessionId, processingTime);
            
            return response;
            
        } catch (Exception e) {
            log.error("模拟AI聊天处理失败", e);
            
            // 返回错误响应
            AiChatResponseVO errorResponse = new AiChatResponseVO();
            errorResponse.setMessage("抱歉，我现在遇到了一些技术问题，请稍后再试或联系人工客服。");
            errorResponse.setSessionId(request.getSessionId());
            errorResponse.setResponseType("error");
            errorResponse.setResponseTime(LocalDateTime.now());
            errorResponse.setProcessingTime(System.currentTimeMillis() - startTime);
            
            return errorResponse;
        }
    }
    
    /**
     * 生成模拟AI回复
     */
    private String generateMockResponse(AiChatRequestDTO request) {
        String message = request.getMessage().toLowerCase();
        String chatType = request.getChatType();
        
        // 根据消息内容和类型生成不同的回复
        if (message.contains("你好") || message.contains("hello")) {
            return "您好！我是小饿，饿了吧外卖平台的智能客服助手 😊 很高兴为您服务！我可以帮您查找商家、推荐美食、查询订单状态等。有什么需要帮助的吗？";
        }
        
        if ("business".equals(chatType) || message.contains("商家") || message.contains("餐厅") || message.contains("店铺")) {
            return generateBusinessResponse(request);
        }
        
        if ("food".equals(chatType) || message.contains("菜") || message.contains("美食") || message.contains("推荐")) {
            return generateFoodResponse(request);
        }
        
        if ("order".equals(chatType) || message.contains("订单") || message.contains("配送")) {
            return generateOrderResponse(request);
        }
        
        if (message.contains("川菜")) {
            return "🌶️ 为您推荐几家优质川菜馆：\n\n1. 巴蜀风味川菜馆 - 地道麻婆豆腐，起送价28元\n2. 辣妹子川菜 - 招牌水煮鱼片，配送费3元\n3. 成都小厨 - 正宗夫妻肺片，评分4.8分\n\n需要了解更多详情或直接下单吗？";
        }
        
        if (message.contains("清淡")) {
            return "🥗 为您推荐一些清淡健康的菜品：\n\n1. 白灼菜心 - 新鲜爽脆，15元\n2. 蒸蛋羹 - 嫩滑营养，12元\n3. 清汤面条 - 清香可口，18元\n4. 白切鸡 - 原汁原味，25元\n\n这些都比较适合想吃清淡食物的您哦！";
        }
        
        // 默认通用回复
        return generateGeneralResponse(message);
    }
    
    /**
     * 生成商家相关回复
     */
    private String generateBusinessResponse(AiChatRequestDTO request) {
        try {
            // 尝试从数据库获取真实商家信息
            List<Business> businesses = knowledgeBaseUtil.searchBusinesses("", 3);
            if (!businesses.isEmpty()) {
                StringBuilder response = new StringBuilder("🏪 为您推荐以下优质商家：\n\n");
                for (int i = 0; i < Math.min(3, businesses.size()); i++) {
                    Business business = businesses.get(i);
                    response.append(i + 1).append(". ").append(knowledgeBaseUtil.formatBusinessInfo(business)).append("\n\n");
                }
                response.append("需要了解更多信息或直接下单吗？");
                return response.toString();
            }
        } catch (Exception e) {
            log.warn("获取商家信息失败，使用模拟数据", e);
        }
        
        return "🏪 为您推荐几家热门商家：\n\n1. 美味轩中餐厅 - 经典粤菜，起送价30元，配送费5元\n2. 意式披萨屋 - 正宗意大利风味，起送价25元，配送费3元\n3. 日式料理店 - 新鲜刺身，起送价50元，配送费8元\n\n所有商家都有很高的用户评价，您想了解哪家的详细信息呢？";
    }
    
    /**
     * 生成菜品相关回复
     */
    private String generateFoodResponse(AiChatRequestDTO request) {
        try {
            // 尝试从数据库获取真实菜品信息
            List<Food> foods = knowledgeBaseUtil.searchFoods("", 3);
            if (!foods.isEmpty()) {
                StringBuilder response = new StringBuilder("🍽️ 为您推荐以下热门菜品：\n\n");
                for (int i = 0; i < Math.min(3, foods.size()); i++) {
                    Food food = foods.get(i);
                    response.append(i + 1).append(". ").append(knowledgeBaseUtil.formatFoodInfo(food)).append("\n\n");
                }
                response.append("想要了解更多菜品或直接下单吗？");
                return response.toString();
            }
        } catch (Exception e) {
            log.warn("获取菜品信息失败，使用模拟数据", e);
        }
        
        return "🍽️ 为您推荐今日热门菜品：\n\n1. 宫保鸡丁 - 经典川菜，香辣下饭，28元\n2. 糖醋里脊 - 酸甜可口，老少皆宜，32元\n3. 麻婆豆腐 - 麻辣鲜香，佐餐佳品，22元\n4. 白切鸡 - 清淡营养，蘸料丰富，35元\n\n这些都是平台上评价很高的菜品，您想尝试哪一道呢？";
    }
    
    /**
     * 生成订单相关回复
     */
    private String generateOrderResponse(AiChatRequestDTO request) {
        try {
            if (request.getUserId() != null) {
                Map<String, Object> userContext = knowledgeBaseUtil.getUserContext(request.getUserId());
                if (userContext.containsKey("lastOrderId")) {
                    Long orderId = (Long) userContext.get("lastOrderId");
                    Integer orderState = (Integer) userContext.get("lastOrderState");
                    
                    String stateText = switch (orderState != null ? orderState : 0) {
                        case 0 -> "待支付";
                        case 1 -> "已支付，商家正在准备中";
                        case 2 -> "配送中，预计20分钟内送达";
                        case 3 -> "已送达";
                        case 4 -> "已取消";
                        default -> "未知状态";
                    };
                    
                    return String.format("📦 您的订单状态如下：\n\n订单号：%d\n当前状态：%s\n\n如有任何问题，请随时联系我们的客服热线：400-888-8888", orderId, stateText);
                }
            }
        } catch (Exception e) {
            log.warn("获取订单信息失败，使用通用回复", e);
        }
        
        return "📦 关于订单查询：\n\n1. 您可以提供订单号，我帮您查询具体状态\n2. 一般情况下，订单流程如下：\n   • 下单支付 → 商家接单 → 制作中 → 配送中 → 已送达\n3. 如果订单超时未送达，我们会为您联系配送员\n\n请提供您的订单号，或告诉我您遇到的具体问题？";
    }
    
    /**
     * 生成通用回复
     */
    private String generateGeneralResponse(String message) {
        if (message.contains("帮助") || message.contains("功能")) {
            return "🤖 我是小饿，您的专属外卖助手！我可以为您提供以下服务：\n\n📋 服务功能：\n• 商家推荐和查询\n• 菜品推荐和搜索\n• 订单状态查询\n• 优惠活动信息\n• 配送时间预估\n• 客服问题解答\n\n🕐 服务时间：7:00-23:00\n📞 人工客服：400-888-8888\n\n有什么具体需要帮助的吗？";
        }
        
        if (message.contains("投诉") || message.contains("问题")) {
            return "😔 很抱歉给您带来不好的体验！我们非常重视每一位用户的反馈。\n\n🔧 处理方式：\n1. 我会立即为您记录问题详情\n2. 转接专业客服为您处理\n3. 48小时内给您满意答复\n\n📞 也可直接拨打客服热线：400-888-8888\n\n请告诉我具体遇到了什么问题，我来帮您解决！";
        }
        
        return "💬 感谢您的咨询！我是饿了吧的智能客服小饿。\n\n我可以帮您：\n• 🏪 查找和推荐商家\n• 🍽️ 推荐美味菜品\n• 📦 查询订单状态\n• ❓ 解答各种问题\n\n请告诉我您需要什么帮助，我会尽力为您服务！";
    }
    
    /**
     * 保存对话历史
     */
    private void saveChatHistory(AiChatRequestDTO request, AiChatResponseVO response, long processingTime) {
        try {
            AiChatHistory history = new AiChatHistory();
            history.setUserId(request.getUserId());
            history.setSessionId(response.getSessionId());
            history.setUserMessage(request.getMessage());
            history.setAiResponse(response.getMessage());
            history.setChatType(request.getChatType());
            history.setProcessingTime(processingTime);
            history.setCreateTime(LocalDateTime.now());
            history.setCreator(request.getUserId());
            history.setIsDeleted(false);
            
            // 构建上下文数据
            Map<String, Object> contextData = Map.of(
                "responseType", response.getResponseType(),
                "needConfirmation", response.getNeedConfirmation() != null ? response.getNeedConfirmation() : false,
                "mockService", true
            );
            
            try {
                history.setContextData(objectMapper.writeValueAsString(contextData));
            } catch (JsonProcessingException e) {
                log.warn("序列化上下文数据失败", e);
                history.setContextData("{}");
            }
            
            chatHistoryMapper.insert(history);
            
        } catch (Exception e) {
            log.error("保存对话历史失败", e);
        }
    }
    
    @Override
    public List<AiChatHistoryVO> getChatHistory(Long userId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1 || size > 50) size = 20;
        
        int offset = (page - 1) * size;
        List<AiChatHistory> histories = chatHistoryMapper.selectByUserId(userId, size, offset);
        
        return histories.stream()
                .map(this::convertToVO)
                .toList();
    }
    
    @Override
    public List<AiChatHistoryVO> getChatHistoryBySession(String sessionId) {
        List<AiChatHistory> histories = chatHistoryMapper.selectBySessionId(sessionId);
        return histories.stream()
                .map(this::convertToVO)
                .toList();
    }
    
    @Override
    @Transactional
    public Boolean deleteChatHistory(Long historyId, Long userId) {
        try {
            Long currentUserId = getCurrentUserId();
            if (currentUserId == null) {
                currentUserId = userId;
            }
            
            int result = chatHistoryMapper.deleteById(historyId, currentUserId);
            return result > 0;
        } catch (Exception e) {
            log.error("删除对话历史失败", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public Boolean cleanOldChatHistory(Long userId, Integer keepCount) {
        try {
            if (keepCount == null || keepCount < 10) {
                keepCount = 50;
            }
            
            int result = chatHistoryMapper.cleanOldRecords(userId, keepCount);
            return result >= 0;
        } catch (Exception e) {
            log.error("清理旧对话历史失败", e);
            return false;
        }
    }
    
    /**
     * 转换为VO对象
     */
    private AiChatHistoryVO convertToVO(AiChatHistory history) {
        AiChatHistoryVO vo = new AiChatHistoryVO();
        BeanUtils.copyProperties(history, vo);
        return vo;
    }
}
