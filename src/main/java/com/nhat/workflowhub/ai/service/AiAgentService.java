package com.nhat.workflowhub.ai.service;

import com.nhat.workflowhub.ai.dto.AiChatRequest;
import com.nhat.workflowhub.ai.dto.AiChatResponse;
import com.nhat.workflowhub.ai.guardrail.AgentRateLimitGuard;
import com.nhat.workflowhub.ai.guardrail.PromptSafetyGuard;
import com.nhat.workflowhub.ai.tool.ToolRegistry;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiAgentService {

  private final AgentRateLimitGuard rateLimitGuard;
  private final PromptSafetyGuard promptSafetyGuard;
  private final ToolRegistry toolRegistry;

  public AiAgentService(
      AgentRateLimitGuard rateLimitGuard,
      PromptSafetyGuard promptSafetyGuard,
      ToolRegistry toolRegistry
  ) {
    this.rateLimitGuard = rateLimitGuard;
    this.promptSafetyGuard = promptSafetyGuard;
    this.toolRegistry = toolRegistry;
  }

  public AiChatResponse chat(UUID currentUserId, AiChatRequest request) {
    AgentRateLimitGuard.Decision rateLimitDecision = rateLimitGuard.allow(currentUserId);
    if (!rateLimitDecision.allowed()) {
      return new AiChatResponse(
          true,
          "You are sending requests too quickly. Please wait before trying again.",
          "rate_limited",
          null,
          0,
          toolRegistry.allowedTools().stream().map(tool -> tool.name()).toList()
      );
    }

    PromptSafetyGuard.Decision safetyDecision = promptSafetyGuard.evaluate(request.message());
    if (!safetyDecision.allowed()) {
      return new AiChatResponse(
          true,
          "I can only help with supported customer support actions inside this workspace.",
          "blocked_prompt",
          null,
          rateLimitDecision.remainingRequests(),
          toolRegistry.allowedTools().stream().map(tool -> tool.name()).toList()
      );
    }

    String normalized = request.message().toLowerCase(Locale.ROOT);
    String suggestedAction;
    String suggestedRoute;
    String message;

    if (normalized.contains("notification")) {
      suggestedAction = "navigate_to";
      suggestedRoute = buildRoute(request.organizationSlug(), "/notifications");
      message = "Opening notifications for this workspace is the safest next step.";
    } else if (normalized.contains("knowledge") || normalized.contains("playbook")) {
      suggestedAction = "navigate_to";
      suggestedRoute = buildRoute(request.organizationSlug(), "/knowledge-base");
      message = "I can help you review workspace knowledge and cite the relevant source.";
    } else if (normalized.contains("create") && normalized.contains("request")) {
      suggestedAction = "create_workflow_item";
      suggestedRoute = buildRoute(request.organizationSlug(), "/customer-requests");
      message = "I can help create a support request. Open the customer requests page to continue.";
    } else if (normalized.contains("status") || normalized.contains("update")) {
      suggestedAction = "update_workflow_status";
      suggestedRoute = buildRoute(request.organizationSlug(), "/customer-requests");
      message = "I can help update a request status from the request detail view.";
    } else {
      suggestedAction = "list_workflow_items";
      suggestedRoute = buildRoute(request.organizationSlug(), "/workflow-items");
      message = "I can help with requests, notifications, and workspace knowledge in this organization.";
    }

    return new AiChatResponse(
        false,
        message,
        suggestedAction,
        suggestedRoute,
        rateLimitDecision.remainingRequests(),
        toolRegistry.allowedTools().stream().map(tool -> tool.name()).toList()
    );
  }

  private String buildRoute(String organizationSlug, String suffix) {
    if (!StringUtils.hasText(organizationSlug)) {
      return suffix;
    }
    return "/orgs/" + organizationSlug + suffix;
  }
}
