package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.PreferenceUpdateDTO;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.entity.UserPreference;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.PreferenceMapper;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.CurrentUserService;
import com.tju.elm_bk.vo.PreferenceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class PreferenceController {
    private final PreferenceMapper preferenceMapper;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public HttpResult<PreferenceVO> me() {
        User user = current();
        UserPreference preference = preferenceMapper.findByUserId(user.getId());
        if (preference == null) preference = defaults();
        return HttpResult.success(toVO(preference));
    }

    @PutMapping("/me")
    public HttpResult<PreferenceVO> update(@RequestBody PreferenceUpdateDTO dto) {
        if (dto == null || dto.getSpicyLevel() == null || dto.getSpicyLevel() < 0 || dto.getSpicyLevel() > 3) {
            throw new APIException("辣度请选择0-3级");
        }
        if (!validLength(dto.getTasteTags(), 200) || !validLength(dto.getAvoidTags(), 200) || !validLength(dto.getCategoryTags(), 200)) {
            throw new APIException("偏好标签不能超过200个字符");
        }
        if (dto.getTheme() != null && !List.of("light", "dark", "mint", "warm").contains(dto.getTheme())) {
            throw new APIException("不支持的页面主题");
        }
        User user = current();
        UserPreference preference = new UserPreference();
        preference.setUserId(user.getId());
        preference.setTheme(dto.getTheme() == null ? "light" : dto.getTheme());
        preference.setSpicyLevel(dto.getSpicyLevel());
        preference.setTasteTags(trim(dto.getTasteTags()));
        preference.setAvoidTags(trim(dto.getAvoidTags()));
        preference.setCategoryTags(trim(dto.getCategoryTags()));
        preferenceMapper.upsert(preference);
        return me();
    }

    @DeleteMapping("/me")
    public HttpResult<Void> clear() {
        preferenceMapper.deleteByUserId(current().getId());
        return HttpResult.success();
    }

    private UserPreference defaults() {
        UserPreference preference = new UserPreference();
        preference.setTheme("light");
        preference.setSpicyLevel(0);
        preference.setTasteTags("");
        preference.setAvoidTags("");
        preference.setCategoryTags("");
        return preference;
    }

    private PreferenceVO toVO(UserPreference preference) {
        return new PreferenceVO(preference.getTheme(), preference.getSpicyLevel(),
                preference.getTasteTags(), preference.getAvoidTags(), preference.getCategoryTags());
    }

    private User current() {
        return currentUserService.requireUser();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean validLength(String value, int max) {
        return value == null || value.length() <= max;
    }
}
