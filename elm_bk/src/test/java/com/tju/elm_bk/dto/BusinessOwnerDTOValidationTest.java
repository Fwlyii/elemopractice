package com.tju.elm_bk.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BusinessOwnerDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidUsername() {
        // 测试有效的用户名
        BusinessOwnerDTO dto = new BusinessOwnerDTO();
        dto.setUsername("validuser");
        
        Set<ConstraintViolation<BusinessOwnerDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "有效用户名应该通过验证");
    }

    @Test
    void testEmptyUsername() {
        // 测试空用户名
        BusinessOwnerDTO dto = new BusinessOwnerDTO();
        dto.setUsername("");
        
        Set<ConstraintViolation<BusinessOwnerDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "空用户名应该验证失败");
        
        ConstraintViolation<BusinessOwnerDTO> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("不能为空"));
    }

    @Test
    void testNullUsername() {
        // 测试null用户名
        BusinessOwnerDTO dto = new BusinessOwnerDTO();
        dto.setUsername(null);
        
        Set<ConstraintViolation<BusinessOwnerDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "null用户名应该验证失败");
        
        ConstraintViolation<BusinessOwnerDTO> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("不能为空"));
    }

    @Test
    void testUsernameTooLong() {
        // 测试用户名过长（超过100个字符）
        BusinessOwnerDTO dto = new BusinessOwnerDTO();
        dto.setUsername("a".repeat(101)); // 101个字符
        
        Set<ConstraintViolation<BusinessOwnerDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "过长的用户名应该验证失败");
        
        ConstraintViolation<BusinessOwnerDTO> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("长度必须在1-100个字符之间"));
    }

    @Test
    void testUsernameMinLength() {
        // 测试最小长度（1个字符）
        BusinessOwnerDTO dto = new BusinessOwnerDTO();
        dto.setUsername("a");
        
        Set<ConstraintViolation<BusinessOwnerDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "1个字符的用户名应该通过验证");
    }

    @Test
    void testUsernameMaxLength() {
        // 测试最大长度（100个字符）
        BusinessOwnerDTO dto = new BusinessOwnerDTO();
        dto.setUsername("a".repeat(100)); // 100个字符
        
        Set<ConstraintViolation<BusinessOwnerDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "100个字符的用户名应该通过验证");
    }

    @Test
    void testUsernameWithSpaces() {
        // 测试包含空格的用户名
        BusinessOwnerDTO dto = new BusinessOwnerDTO();
        dto.setUsername("  "); // 只有空格
        
        Set<ConstraintViolation<BusinessOwnerDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "只有空格的用户名应该验证失败");
        
        ConstraintViolation<BusinessOwnerDTO> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("不能为空"));
    }
}
