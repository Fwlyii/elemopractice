package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeepSeekResponseDTO {
    
    private String id;
    
    private String object;
    
    private Long created;
    
    private String model;
    
    private List<ChoiceDTO> choices;
    
    private UsageDTO usage;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChoiceDTO {
        private Integer index;
        
        private MessageDTO message;
        
        @JsonProperty("finish_reason")
        private String finishReason;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageDTO {
        private String role;
        private String content;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UsageDTO {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
