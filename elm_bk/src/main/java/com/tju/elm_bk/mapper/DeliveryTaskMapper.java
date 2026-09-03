package com.tju.elm_bk.mapper;

import com.tju.elm_bk.entity.DeliveryException;
import com.tju.elm_bk.entity.DeliveryTask;
import com.tju.elm_bk.vo.DeliveryExceptionVO;
import com.tju.elm_bk.vo.DeliveryTaskVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeliveryTaskMapper {
    void insertTask(DeliveryTask task);

    DeliveryTask selectById(Long id);

    DeliveryTask selectByOrderId(Long orderId);

    DeliveryTaskVO selectViewById(Long id);

    DeliveryTaskVO selectViewByOrderId(Long orderId);

    List<DeliveryTaskVO> listAvailable();

    List<DeliveryTaskVO> listByRider(@Param("riderUserId") Long riderUserId,
                                     @Param("active") Boolean active);

    int acceptTask(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int markArrivedStore(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int markPickedUp(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int markDelivered(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int markCompleted(@Param("taskId") Long taskId);

    int autoCompleteDelivered(@Param("orderId") Long orderId);

    int markException(@Param("taskId") Long taskId, @Param("riderUserId") Long riderUserId);

    int resumeFromException(@Param("taskId") Long taskId, @Param("targetStatus") String targetStatus);

    int reassignFromException(@Param("taskId") Long taskId);

    int cancelFromException(@Param("taskId") Long taskId);

    int cancelWaitingTask(@Param("taskId") Long taskId);

    int countActiveByRider(@Param("riderUserId") Long riderUserId);

    void insertException(DeliveryException deliveryException);

    DeliveryException selectExceptionById(Long id);

    DeliveryException selectOpenExceptionByTask(Long taskId);

    List<DeliveryExceptionVO> listExceptions(@Param("status") Integer status);

    int resolveException(@Param("id") Long id,
                         @Param("action") String action,
                         @Param("note") String note,
                         @Param("resolverUserId") Long resolverUserId);
}
