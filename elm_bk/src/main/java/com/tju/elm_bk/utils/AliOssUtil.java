package com.tju.elm_bk.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AliOssUtil {
    private static final Logger LOG = LoggerFactory.getLogger(AliOssUtil.class);
    private static final String ENDPOINT = "https://oss-cn-beijing.aliyuncs.com";
    private static final String ACCESS_KEY_ID = requireEnvironmentVariable("ALIYUN_OSS_ACCESS_KEY_ID");
    private static final String SECRET_ACCESS_KEY = requireEnvironmentVariable("ALIYUN_OSS_ACCESS_KEY_SECRET");
    private static final String BUCKET_NAME = "sunnybigevent";

    private static String requireEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }

    //上传文件,返回文件的公网访问地址
    public static String uploadFile(String objectName, InputStream inputStream){
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(ENDPOINT,ACCESS_KEY_ID,SECRET_ACCESS_KEY);
        //公文访问地址
        String url = "";
        try {
            // 创建存储空间。
            ossClient.createBucket(BUCKET_NAME);
            ossClient.putObject(BUCKET_NAME, objectName, inputStream);
            url = "https://"+BUCKET_NAME+"."+ENDPOINT.substring(ENDPOINT.lastIndexOf("/")+1)+"/"+objectName;
        } catch (OSSException oe) {
            LOG.error("OSS拒绝上传，错误码: {}, requestId: {}", oe.getErrorCode(), oe.getRequestId());
        } catch (ClientException ce) {
            LOG.error("OSS客户端上传失败: {}", ce.getClass().getSimpleName());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return url;
    }
}
