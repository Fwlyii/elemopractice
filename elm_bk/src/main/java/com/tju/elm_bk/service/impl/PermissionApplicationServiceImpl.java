package com.tju.elm_bk.service.impl;

import com.tju.elm_bk.dto.AuditPermissionDTO;
import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessPermissionDTO;
import com.tju.elm_bk.entity.Authority;
import com.tju.elm_bk.entity.Notification;
import com.tju.elm_bk.entity.PermissionApplication;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.*;
import com.tju.elm_bk.service.PermissionApplicationService;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.BusinessPermissionVO;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.MerchantApplicationsVO;
import com.tju.elm_bk.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSONObject;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PermissionApplicationServiceImpl implements PermissionApplicationService {
    @Autowired
    private PermissionApplicationMapper applicationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WebSocketServer webSocketServer; // 注入WebSocket服务

    @Autowired
    private AuthorityMapper authorityMapper;

    @Autowired
    private UserAuthorityMapper userAuthorityMapper;

    @Autowired
    private BusinessMapper businessMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    /**
     * 顾客申请成为商家
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionApplication applyMerchant() {
        // 1. 获取当前登录用户ID
        String currentUsername = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("未获取到当前登录用户名"));
        Long currentUserId = userMapper.getUserIdByUsername(currentUsername);
        log.info("当前用户ID: {}", currentUserId);

        // 2. 校验用户是否存在
        User currentUser = userMapper.findById(currentUserId);
        if (currentUser == null) {
            throw new APIException("当前用户不存在");
        }

        // 3. 检查是否已提交过未审核的申请（避免重复申请）
        int existingCount = applicationMapper.countByUserId(currentUserId);
        if (existingCount > 0) {
            throw new APIException("您已提交过申请，请等待管理员审核");
        }

        // 4. 构建申请记录
        PermissionApplication application = new PermissionApplication();
        application.setUserId(currentUserId);
        application.setStatus(0);
        application.setIsDeleted(false);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());

        // 5. 保存到数据库
        applicationMapper.insert(application);

        //6. 通过WebSocket向管理员推送消息
        try {
            sendMerchantApplyNotification(currentUserId, currentUser.getUsername(), application.getId());
        } catch (JSONException e) {
            throw new APIException("消息发送失败");
        }
        return application;
    }

    /**
     * 管理员审核成为商家申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionApplication auditApplication(AuditPermissionDTO auditDTO) {

        String currentUsername = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("未获取到当前登录用户名"));
        Long currentUserId = userMapper.getUserIdByUsername(currentUsername);

        // 2. 查询申请记录是否存在
        PermissionApplication application = applicationMapper.selectById(auditDTO.getId());
        if (application == null) {
            throw new APIException("申请记录不存在");
        }
        // 校验申请状态（只能审核未审核的记录）
        if (application.getStatus() != 0) {
            throw new APIException("该申请已审核，无需重复操作");
        }

        // 3. 更新申请状态
        application.setStatus(auditDTO.getAuditResult());// 1-同意，2-拒绝
        application.setUpdateTime(LocalDateTime.now());
        applicationMapper.updateAuditStatus(application);

        // 4. 如果同意申请，给用户添加BUSINESS权限
        Long applicantUserId = application.getUserId(); // 申请人ID
        if (auditDTO.getAuditResult() == 1) { // 1-同意
            addBusinessAuthority(applicantUserId);
            // 5. 推送WebSocket通知给申请人
            sendAuditPassNotification(applicantUserId,0);
        } else {
            // 若拒绝，可选择性推送拒绝通知
            sendAuditRejectNotification(applicantUserId,0);
        }
        return application;
    }


    /**
     * 顾客申请开店
     **/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessPermissionVO applyShop(BusinessPermissionDTO businessPermissionDTO) {
        String currentUsername = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("未获取到当前登录用户名"));
        Long currentUserId = userMapper.getUserIdByUsername(currentUsername);
        log.info("当前用户ID: {}", currentUserId);

        User currentUser = userMapper.findById(currentUserId);
        if (currentUser == null) {
            throw new APIException("当前用户不存在");
        }
        businessPermissionDTO.setStatus(0);
        businessPermissionDTO.setUserId(currentUserId);
        businessPermissionDTO.setCreator(currentUserId);
        businessPermissionDTO.setUpdater(currentUserId);
        businessPermissionDTO.setCreateTime(LocalDateTime.now());
        businessPermissionDTO.setUpdateTime(LocalDateTime.now());
        businessMapper.insertBusinessPermission(businessPermissionDTO);
        try {
            sendShopApplyNotification(currentUserId, currentUser.getUsername());
        } catch (JSONException e) {
            throw new APIException("消息发送失败");
        }
        BusinessPermissionVO businessPermissionVO = new BusinessPermissionVO();
        BeanUtils.copyProperties(businessPermissionDTO, businessPermissionVO);
        return businessPermissionVO;
    }

    @Override
    public BusinessPermissionVO auditShopApplication(BusinessPermissionDTO businessPermissionDTO) {
        String currentUsername = SecurityUtils.getCurrentUsername()
                .orElseThrow(() -> new APIException("未获取到当前登录用户名"));
        Long currentUserId = userMapper.getUserIdByUsername(currentUsername);
        businessPermissionDTO.setUpdater(currentUserId);
        businessPermissionDTO.setUpdateTime(LocalDateTime.now());
        if (businessMapper.selectBusinessById(businessPermissionDTO.getId()) == null) {
            throw new APIException("申请记录不存在");
        }
        businessMapper.updateBusinessStatus(businessPermissionDTO);
        BusinessPermissionVO businessPermissionVO =businessMapper.getBusinessPermissionById(businessPermissionDTO.getId());
        Long applicantUserId = businessPermissionVO.getUserId();
        if (businessPermissionDTO.getStatus() == 1) { // 1-同意
            sendAuditPassNotification(applicantUserId,1);
        } else {
            // 若拒绝，可选择性推送拒绝通知
            sendAuditRejectNotification(applicantUserId,1);
        }
        return businessPermissionVO;
    }

    @Override
    public List<MerchantApplicationsVO> getMerchantApplications() {
        List<PermissionApplication> applications = applicationMapper.list();
        List<MerchantApplicationsVO> merchantApplications = new ArrayList<>();
        for (PermissionApplication application : applications) {
            merchantApplications.add(new MerchantApplicationsVO(
                    application.getId(),
                    application.getUserId(),
                    userMapper.findById(application.getUserId()).getUsername(),
                    application.getCreateTime()
            ));
        }
        return merchantApplications;
    }

    @Override
    public List<BusinessPermissionVO> getShopApplications() {
        List<BusinessPermissionVO> applications =businessMapper.listNotAudited();
        return applications;
    }

    /**
     * 给用户添加BUSINESS权限（避免重复添加）
     */
    private void addBusinessAuthority(Long userId) {
        String businessAuthority = "BUSINESS";
        // 检查权限是否存在
        Authority authority = authorityMapper.findByName(businessAuthority);
        if (authority == null) {
            throw new APIException("系统中不存在BUSINESS权限，请先配置");
        }
        // 检查用户是否已拥有该权限
        int existing = userAuthorityMapper.countByUserIdAndAuthority(userId, businessAuthority);
        if (existing > 0) {
            throw new APIException("该用户已拥有商家权限");
        }
        // 新增权限关联
        userAuthorityMapper.insertUserAuthority(userId, businessAuthority);
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 推送"审核通过"通知给顾客
     */
    private void sendAuditPassNotification(Long userId,Integer type) {
        JSONObject message = new JSONObject();
        message.put("currentTime", LocalDateTime.now().format(TIME_FORMATTER));
        Notification notification = new Notification();
        if(type==0){
            String content = "恭喜！您的成为商家申请已通过审核，现在可以开始营业了";
            notification.setNotificationContent(content);
            message.put("type", 0); // 0表示申请成为商家的回复
            message.put("content",content);
        }else if(type==1){
            String content = "恭喜！您的开店申请已通过审核，现在可以开始营业了";
            notification.setNotificationContent(content);
            message.put("type", 1); // 1表示申请开店的回复
            message.put("content", content);
        }
        message.put("userId", userId);
        webSocketServer.sendToAllClient(message.toJSONString());

        notification.setUserId(userId); // 接收消息的用户ID
        notification.setNotificationType(type); // 0=商家申请，1=开店申请
        notification.setAuditResult(1); // 1=通过，2=拒绝
        notification.setIsRead(0);
        notification.setIsDeleted(0);
        notification.setCreateTime(LocalDateTime.now()); // 当前时间
        notificationMapper.insert(notification); // 插入数据库
    }

    /**
     * 推送"审核拒绝"通知给顾客
     */
    private void sendAuditRejectNotification(Long userId,Integer type) {
        JSONObject message = new JSONObject();
        message.put("currentTime", LocalDateTime.now().format(TIME_FORMATTER));
        Notification notification = new Notification();
        if(type==0){
            String content = "抱歉，您的成为商家申请未通过审核";
            notification.setNotificationContent(content);
            message.put("type", 0); // 0表示申请成为商家的回复
            message.put("content", content);
        }else if(type==1){
            String content = "抱歉，您的开店申请未通过审核";
            notification.setNotificationContent(content);
            message.put("type", 1); // 1表示申请开店的回复
            message.put("content", content);
        }
        message.put("userId", userId);
        webSocketServer.sendToAllClient(message.toJSONString());
        notification.setUserId(userId); // 接收消息的用户ID
        notification.setNotificationType(type); // 0=商家申请，1=开店申请
        notification.setAuditResult(2); // 1=通过，2=拒绝
        notification.setIsRead(0);
        notification.setIsDeleted(0);
        notification.setCreateTime(LocalDateTime.now()); // 当前时间
        notificationMapper.insert(notification); // 插入数据库
    }

    /**
     * 向管理员推送新申请通知
     * @param userId 申请人ID
     * @param username 申请人用户名
     */
    private void sendMerchantApplyNotification(Long userId, String username,Long applicationId) throws JSONException {
        // 构建消息体（包含type、userId、content）
        JSONObject message = new JSONObject();
        message.put("applicationId", applicationId);
        message.put("currentTime", LocalDateTime.now().format(TIME_FORMATTER));
        message.put("type", 0); // 0表示申请成为商家
        message.put("userId", userId);
        message.put("content", "用户[" + username + "]申请成为商家，请及时审核");

        // 调用WebSocket服务群发消息（管理员客户端会监听该消息）
        webSocketServer.sendToAllClient(message.toJSONString());
    }
    /**
     * 向管理员推送开店申请通知
     * @param userId 申请人ID
     * @param username 申请人用户名
     */
    private void sendShopApplyNotification(Long userId, String username) throws JSONException {
        // 构建消息体（包含type、userId、content）
        JSONObject message = new JSONObject();
        message.put("currentTime", LocalDateTime.now().format(TIME_FORMATTER));
        message.put("type", 1); // 1表示申请开店
        message.put("userId", userId);
        message.put("content", "商家[" + username + "]申请开店，请及时审核");

        // 调用WebSocket服务群发消息（管理员客户端会监听该消息）
        webSocketServer.sendToAllClient(message.toJSONString());
    }
}
