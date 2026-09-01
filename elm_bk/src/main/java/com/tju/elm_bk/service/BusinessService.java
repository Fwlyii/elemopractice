package com.tju.elm_bk.service;


import com.tju.elm_bk.dto.BusinessDTO;
import com.tju.elm_bk.dto.BusinessInfoDTO;
import com.tju.elm_bk.dto.BusinessUpdateDTO;

import com.tju.elm_bk.entity.Business;
import com.tju.elm_bk.vo.BusinessSearchVO;
import com.tju.elm_bk.vo.BusinessVO;
import com.tju.elm_bk.vo.MerchantStatsVO;

import java.util.List;

public interface BusinessService {
    BusinessVO getBusinessById(Long id);
    BusinessVO updateBusiness(Long id, BusinessUpdateDTO updateDto);
    BusinessVO deleteBusiness(Long id);
    BusinessVO patchBusiness(Long id, BusinessUpdateDTO updateDto);
    List<BusinessVO> getBusinesses();
    BusinessVO addBusiness(BusinessDTO businessDto);
    List<BusinessSearchVO> getBusinessesBySearch(String keyword, boolean isScore,boolean isSales);
    Integer applyForAddBusiness(Business  business);
    List<BusinessInfoDTO> getAllActiveBusinesses();
    List<Business> getMerchantBusinesses(Long userId, Integer status);
    List<Business>listBusinessByOrderTypeId(Integer type);
    List<MerchantStatsVO> getBusinessIdList();
    List<BusinessSearchVO> getBusinessesInCarousel();
    public BusinessVO patchBusinessOwn(Long id, BusinessUpdateDTO updateDto);
}
