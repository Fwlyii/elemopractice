package com.tju.elm_bk.controller;

import com.tju.elm_bk.result.HttpResult;
import com.tju.elm_bk.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@Tag(name="文件上传")
@RequiredArgsConstructor
public class FileUploadController {
    private final ImageStorageService imageStorageService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public HttpResult<String> uploadFile(MultipartFile file) throws IOException {
        return HttpResult.success(imageStorageService.storeImage(file));
    }
}
