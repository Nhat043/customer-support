package com.nhat.workflowhub.ai.guardrail;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PromptSafetyGuard {

  private static final List<String> BLOCKED_PHRASES = List.of(
      "ignore previous instructions",
      "ignore all previous instructions",
      "system prompt",
      "developer message",
      "dump the database",
      "export all records",
      "other tenant",
      "another tenant",
      "another organization",
      "drop table",
      "delete from",
      "show me the hidden prompt"
  );

  public Decision evaluate(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return new Decision(false, "Prompt is empty");
    }

    String normalized = prompt.toLowerCase(Locale.ROOT);
    for (String blockedPhrase : BLOCKED_PHRASES) {
      if (normalized.contains(blockedPhrase)) {
        return new Decision(false, "Prompt injection or data exfiltration attempt blocked");
      }
    }

    return new Decision(true, null);
  }

  public record Decision(boolean allowed, String reason) {
  }
}
