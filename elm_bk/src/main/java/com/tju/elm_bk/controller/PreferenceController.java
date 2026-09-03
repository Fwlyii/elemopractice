package com.tju.elm_bk.controller;

import com.tju.elm_bk.dto.PreferenceUpdateDTO;
import com.tju.elm_bk.entity.User;
import com.tju.elm_bk.entity.UserPreference;
import com.tju.elm_bk.exception.APIException;
import com.tju.elm_bk.mapper.PreferenceMapper;
import com.tju.elm_bk.mapper.UserMapper;
import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.utils.SecurityUtils;
import com.tju.elm_bk.vo.PreferenceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class PreferenceController {
    private final PreferenceMapper preferenceMapper;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public HttpResult<PreferenceVO> me() {
        User user = current();
        UserPreference p = preferenceMapper.findByUserId(user.getId());
        if (p == null) p = defaults();
        return HttpResult.success(toVO(p));
    }

    @PutMapping("/me")
    public HttpResult<PreferenceVO> update(@RequestBody PreferenceUpdateDTO dto) {
        if (dto == null || dto.getSpicyLevel() == null || dto.getSpicyLevel() < 0 || dto.getSpicyLevel() > 3) {
            throw new APIException("辣度请选择0-3级");
        }
        if (!validLength(dto.getTasteTags(), 200) || !validLength(dto.getAvoidTags(), 200) || !validLength(dto.getCategoryTags(), 200)) {
            throw new APIException("偏好标签不能超过200个字符");
        }
        if (dto.getTheme() != null && !dto.getTheme().equals("light") && !dto.getTheme().equals("dark")) {
            throw new APIException("主题仅支持浅色或深色");
        }
        User user = current();
        UserPreference p = new UserPreference(); p.setUserId(user.getId());
        p.setTheme(dto.getTheme() == null ? "light" : dto.getTheme()); p.setSpicyLevel(dto.getSpicyLevel());
        p.setTasteTags(trim(dto.getTasteTags())); p.setAvoidTags(trim(dto.getAvoidTags())); p.setCategoryTags(trim(dto.getCategoryTags()));
        preferenceMapper.upsert(p);
        return me();
    }

    @DeleteMapping("/me")
    public HttpResult<Void> clear() { preferenceMapper.deleteByUserId(current().getId()); return HttpResult.success(); }

    private UserPreference defaults() { UserPreference p = new UserPreference(); p.setTheme("light"); p.setSpicyLevel(0); p.setTasteTags(""); p.setAvoidTags(""); p.setCategoryTags(""); return p; }
    private PreferenceVO toVO(UserPreference p) { return new PreferenceVO(p.getTheme(), p.getSpicyLevel(), p.getTasteTags(), p.getAvoidTags(), p.getCategoryTags()); }
    private User current() { String username = SecurityUtils.getCurrentUsername().orElseThrow(() -> new APIException("请先登录")); User u = userMapper.findByUsername(username); if (u == null) throw new APIException("当前用户不存在"); return u; }
    private String trim(String value) { return value == null ? "" : value.trim(); }
    private boolean validLength(String value, int max) { return value == null || value.length() <= max; }
}
