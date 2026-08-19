package com.nhat.workflowhub.ai.dto;

import java.util.List;

public record AiChatResponse(
    boolean blocked,
    String message,
    String suggestedAction,
    String suggestedRoute,
    long remainingRequests,
    List<String> allowedTools
) {
}
