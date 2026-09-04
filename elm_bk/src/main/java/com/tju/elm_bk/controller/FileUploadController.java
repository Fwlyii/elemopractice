package com.tju.elm_bk.controller;

import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@RestController
@Tag(name="文件上传")
public class FileUploadController {
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public HttpResult<String> uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new com.tju.elm_bk.exception.APIException("请选择需要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new com.tju.elm_bk.exception.APIException("图片大小不能超过5MB");
        }
        byte[] content = file.getBytes();
        String extension = detectImageExtension(content);
        if (extension == null) {
            throw new com.tju.elm_bk.exception.APIException("仅支持 JPG、PNG 或 WebP 图片");
        }
        // 浏览器声明的 MIME 类型和原始文件名都可伪造，扩展名必须由真实文件头决定。
        String fileName = UUID.randomUUID() + extension;
        String url = AliOssUtil.uploadFile(fileName, new ByteArrayInputStream(content));
        if (url == null || url.isBlank()) {
            throw new com.tju.elm_bk.exception.APIException("图片上传失败，请稍后重试");
        }
        return HttpResult.success(url);
    }

    private String detectImageExtension(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return ".jpg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a
                && bytes[6] == 0x1a && bytes[7] == 0x0a) {
            return ".png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}
