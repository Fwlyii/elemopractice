package com.tju.elm_bk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeepSeekRequestDTO {
    
    private String model = "deepseek-chat";
    
    private List<MessageDTO> messages;
    
    private boolean stream = false;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens = 1024;
    
    private Double temperature = 0.7;
    
    @JsonProperty("top_p")
    private Double topP = 0.9;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageDTO {
        private String role; // system, user, assistant
        private String content;
    }
}
