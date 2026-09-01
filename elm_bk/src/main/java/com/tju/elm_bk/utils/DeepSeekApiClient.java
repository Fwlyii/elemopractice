package com.tju.elm_bk.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tju.elm_bk.config.DeepSeekConfig;
import com.tju.elm_bk.dto.DeepSeekRequestDTO;
import com.tju.elm_bk.dto.DeepSeekResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
public class DeepSeekApiClient {
    
    private final DeepSeekConfig deepSeekConfig;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    
    public DeepSeekApiClient(DeepSeekConfig deepSeekConfig, ObjectMapper objectMapper) {
        this.deepSeekConfig = deepSeekConfig;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + deepSeekConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    /**
     * 调用DeepSeek Chat API
     */
    public Mono<DeepSeekResponseDTO> chatCompletion(DeepSeekRequestDTO request) {
        log.info("调用DeepSeek API，请求: {}", formatRequestForLog(request));
        
        return webClient.post()
                .uri(deepSeekConfig.getChatEndpoint())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DeepSeekResponseDTO.class)
                .timeout(Duration.ofSeconds(deepSeekConfig.getTimeoutSeconds()))
                .retryWhen(Retry.backoff(deepSeekConfig.getMaxRetries(), Duration.ofSeconds(1))
                        .filter(this::shouldRetry))
                .doOnSuccess(response -> log.info("DeepSeek API调用成功"))
                .doOnError(error -> log.error("DeepSeek API调用失败: {}", error.getMessage()));
    }
    
    /**
     * 同步调用DeepSeek API
     */
    public DeepSeekResponseDTO chatCompletionSync(DeepSeekRequestDTO request) {
        return chatCompletion(request).block();
    }
    
    /**
     * 判断是否应该重试
     */
    private boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            int statusCode = ex.getStatusCode().value();
            // 对于429(限流)、500、502、503、504错误进行重试
            return statusCode == 429 || statusCode >= 500;
        }
        return true; // 网络错误等其他异常也重试
    }
    
    /**
     * 格式化请求日志（隐藏敏感信息）
     */
    private String formatRequestForLog(DeepSeekRequestDTO request) {
        try {
            // 创建副本用于日志，避免修改原对象
            DeepSeekRequestDTO logRequest = new DeepSeekRequestDTO();
            logRequest.setModel(request.getModel());
            logRequest.setMaxTokens(request.getMaxTokens());
            logRequest.setTemperature(request.getTemperature());
            logRequest.setTopP(request.getTopP());
            logRequest.setStream(request.isStream());
            
            // 只记录消息数量和类型，不记录具体内容
            if (request.getMessages() != null) {
                logRequest.setMessages(request.getMessages().stream()
                        .map(msg -> {
                            DeepSeekRequestDTO.MessageDTO logMsg = new DeepSeekRequestDTO.MessageDTO();
                            logMsg.setRole(msg.getRole());
                            logMsg.setContent("***"); // 隐藏具体内容
                            return logMsg;
                        })
                        .toList());
            }
            
            return objectMapper.writeValueAsString(logRequest);
        } catch (JsonProcessingException e) {
            return "无法序列化请求对象";
        }
    }
}
