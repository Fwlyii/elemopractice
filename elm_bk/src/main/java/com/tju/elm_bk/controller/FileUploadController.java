package com.tju.elm_bk.controller;

import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@Tag(name="文件上传")
public class FileUploadController {
    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public HttpResult<String> uploadFile(MultipartFile file) throws IOException {
        String originFileName = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + originFileName.substring(originFileName.lastIndexOf("."));
        String url= AliOssUtil.uploadFile(fileName, file.getInputStream());
        // 处理文件上传逻辑
        return HttpResult.success(url);
    }
}
