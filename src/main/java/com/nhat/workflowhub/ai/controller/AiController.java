package com.nhat.workflowhub.ai.controller;

import com.nhat.workflowhub.ai.dto.AiChatRequest;
import com.nhat.workflowhub.ai.dto.AiChatResponse;
import com.nhat.workflowhub.ai.service.AiAgentService;
import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

  private final AiAgentService aiAgentService;

  public AiController(AiAgentService aiAgentService) {
    this.aiAgentService = aiAgentService;
  }

  @PostMapping("/chat")
  public AiChatResponse chat(@Valid @RequestBody AiChatRequest request, Authentication authentication) {
    return aiAgentService.chat(currentUserId(authentication), request);
  }

  private UUID currentUserId(Authentication authentication) {
    return ((AuthenticatedUser) authentication.getPrincipal()).userId();
  }
}
